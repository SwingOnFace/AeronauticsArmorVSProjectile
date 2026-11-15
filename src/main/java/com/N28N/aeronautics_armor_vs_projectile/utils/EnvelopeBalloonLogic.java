package com.N28N.aeronautics_armor_vs_projectile.utils;

import com.N28N.aeronautics_armor_vs_projectile.mixin.BalloonStateAccessor;
import com.N28N.aeronautics_armor_vs_projectile.mixin.ServerBalloonGasAccessor;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeBlock;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class EnvelopeBalloonLogic {
    private EnvelopeBalloonLogic() {
    }

    public static Balloon createBalloon(BlockEntityLiftingGasProvider heater, BlockPos startPos) {
        Level level = heater.getLevel();
        Set<BlockPos> envelopes = findConnectedEnvelopes(level, startPos);
        if (envelopes.isEmpty()) {
            return null;
        }

        List<BlockEntityLiftingGasProvider> heaters = List.of(heater);
        BlockPos controllerPos = chooseController(envelopes, heaters, null);
        BalloonLayerGraph graph = buildGraph(envelopes, controllerPos);
        LevelAccelerator accelerator = new LevelAccelerator(level);
        ObjectArrayList<BlockEntityLiftingGasProvider> heaterList = new ObjectArrayList<>(heaters);

        if (level instanceof ServerLevel) {
            return new ServerBalloon(level, accelerator, controllerPos, graph, heaterList);
        }
        return new ClientBalloon(level, accelerator, controllerPos, graph, heaterList);
    }

    public static void rebuildFromController(Balloon balloon) {
        Level level = getLevel(balloon);
        Set<BlockPos> envelopes = findConnectedEnvelopes(level, balloon.getControllerPos());
        if (envelopes.isEmpty()) {
            for (BlockEntityLiftingGasProvider heater : balloon.getHeaters()) {
                BlockPos castPos = heater.getCastPosition();
                if (castPos != null) {
                    envelopes = findConnectedEnvelopes(level, castPos);
                    if (!envelopes.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (envelopes.isEmpty()) {
            invalidate(balloon);
            return;
        }

        List<BlockEntityLiftingGasProvider> oldHeaters = new ArrayList<>(balloon.getHeaters());
        List<BlockEntityLiftingGasProvider> heaters = new ArrayList<>();
        for (BlockEntityLiftingGasProvider heater : oldHeaters) {
            BlockPos castPos = heater.getCastPosition();
            if (castPos != null && isAdjacent(envelopes, castPos)) {
                heaters.add(heater);
            } else {
                heater.setBalloon(null);
            }
        }

        configureBalloon(balloon, new BalloonGroup(envelopes, heaters), balloon.getControllerPos());
        if (level instanceof ServerLevel) {
            BalloonMap.MAP.get(level).markDirty();
        }
    }

    public static void onEnvelopeAdded(Balloon balloon, BlockPos pos) {
        Level level = getLevel(balloon);
        if (!isEnvelope(level, pos)) {
            return;
        }

        Set<BlockPos> known = collectEnvelopeBlocks(balloon);
        boolean connectedToBalloon = false;
        for (Direction direction : Direction.values()) {
            if (known.contains(pos.relative(direction))) {
                connectedToBalloon = true;
                break;
            }
        }
        if (!connectedToBalloon) {
            return;
        }

        known.addAll(findConnectedEnvelopes(level, pos));
        rebuildGroups(balloon, connectedComponents(known));
    }

    public static void onEnvelopeRemoved(Balloon balloon, BlockPos pos) {
        BalloonLayerData layer = balloon.getGraph().getLayerAt(pos);
        if (layer == null || layer.getSolidBlock(pos.getX(), pos.getZ())) {
            return;
        }

        Set<BlockPos> remaining = collectEnvelopeBlocks(balloon);
        rebuildGroups(balloon, connectedComponents(remaining));
    }

    private static void rebuildGroups(Balloon balloon, List<Set<BlockPos>> components) {
        if (components.isEmpty()) {
            invalidate(balloon);
            return;
        }

        Level level = getLevel(balloon);
        List<BlockEntityLiftingGasProvider> oldHeaters = new ArrayList<>(balloon.getHeaters());
        List<BalloonGroup> groups = assignHeaters(components, oldHeaters);
        Map<LiftingGasType, LiftingGasData> gasSnapshot = snapshotGas(balloon);
        int primaryIndex = choosePrimaryGroup(balloon, groups);
        double totalEnvelopeCount = groups.stream().mapToInt(group -> group.envelopes().size()).sum();

        for (int i = 0; i < groups.size(); i++) {
            BalloonGroup group = groups.get(i);
            double share = group.envelopes().size() / totalEnvelopeCount;
            Balloon target = i == primaryIndex ? balloon : createBalloon(balloon, group);
            configureBalloon(target, group, i == primaryIndex ? balloon.getControllerPos() : null);
            distributeGas(target, gasSnapshot, share);
            for (BlockEntityLiftingGasProvider heater : group.heaters()) {
                heater.setBalloon(target);
            }
        }

        Set<BlockEntityLiftingGasProvider> assignedHeaters = new HashSet<>();
        for (BalloonGroup group : groups) {
            assignedHeaters.addAll(group.heaters());
        }
        for (BlockEntityLiftingGasProvider heater : oldHeaters) {
            if (!assignedHeaters.contains(heater)) {
                heater.setBalloon(null);
            }
        }

        if (level instanceof ServerLevel) {
            BalloonMap.MAP.get(level).markDirty();
        }
    }

    private static void configureBalloon(Balloon balloon, BalloonGroup group, BlockPos preferredController) {
        BlockPos controllerPos = chooseController(group.envelopes(), group.heaters(), preferredController);
        BalloonLayerGraph graph = buildGraph(group.envelopes(), controllerPos);
        BalloonStateAccessor access = (BalloonStateAccessor) balloon;
        access.aavp$getHeaters().clear();
        access.aavp$getHeaters().addAll(group.heaters());
        access.aavp$setControllerPos(controllerPos);
        access.aavp$setGraph(graph);
        access.aavp$recomputeBalloonData();
        access.aavp$onRebuilt();
    }

    private static Balloon createBalloon(Balloon source, BalloonGroup group) {
        Level level = getLevel(source);
        BlockPos controllerPos = chooseController(group.envelopes(), group.heaters(), null);
        BalloonLayerGraph graph = buildGraph(group.envelopes(), controllerPos);
        LevelAccelerator accelerator = new LevelAccelerator(level);
        ObjectArrayList<BlockEntityLiftingGasProvider> heaters = new ObjectArrayList<>(group.heaters());

        if (level instanceof ServerLevel) {
            return new ServerBalloon(level, accelerator, controllerPos, graph, heaters);
        }
        return new ClientBalloon(level, accelerator, controllerPos, graph, heaters);
    }

    private static void distributeGas(Balloon balloon, Map<LiftingGasType, LiftingGasData> gasSnapshot,
            double share) {
        if (!(balloon instanceof ServerBalloon serverBalloon)) {
            return;
        }

        Map<LiftingGasType, LiftingGasData> gasAmounts =
                ((ServerBalloonGasAccessor) serverBalloon).aavp$getGasAmounts();
        gasAmounts.clear();
        for (Map.Entry<LiftingGasType, LiftingGasData> entry : gasSnapshot.entrySet()) {
            LiftingGasData oldData = entry.getValue();
            gasAmounts.put(entry.getKey(), new LiftingGasData(0, oldData.amount * share, 0));
        }
    }

    private static Map<LiftingGasType, LiftingGasData> snapshotGas(Balloon balloon) {
        Map<LiftingGasType, LiftingGasData> snapshot = new HashMap<>();
        if (balloon instanceof ServerBalloon serverBalloon) {
            for (Map.Entry<LiftingGasType, LiftingGasData> entry
                    : ((ServerBalloonGasAccessor) serverBalloon).aavp$getGasAmounts().entrySet()) {
                LiftingGasData data = entry.getValue();
                snapshot.put(entry.getKey(), new LiftingGasData(data.target, data.amount, data.nudge));
            }
        }
        return snapshot;
    }

    private static void invalidate(Balloon balloon) {
        Level level = getLevel(balloon);
        BalloonStateAccessor access = (BalloonStateAccessor) balloon;
        for (BlockEntityLiftingGasProvider heater : balloon.getHeaters()) {
            heater.setBalloon(null);
        }
        access.aavp$getHeaters().clear();

        if (balloon instanceof ServerBalloon serverBalloon) {
            ((ServerBalloonGasAccessor) serverBalloon).aavp$getGasAmounts().clear();
        }

        balloon.setLeaking();
        BlockPos controllerPos = balloon.getControllerPos();
        access.aavp$setGraph(new BalloonLayerGraph(controllerPos.getY()));
        access.aavp$setCapacity(0);
        access.aavp$recomputeBalloonData();
        if (balloon instanceof ClientBalloon) {
            access.aavp$onRebuilt();
        }
        if (level instanceof ServerLevel) {
            BalloonMap.MAP.get(level).markDirty();
        }
    }

    private static List<BalloonGroup> assignHeaters(List<Set<BlockPos>> components,
            List<BlockEntityLiftingGasProvider> heaters) {
        List<BalloonGroup> groups = new ArrayList<>(components.size());
        for (Set<BlockPos> component : components) {
            groups.add(new BalloonGroup(component, new ArrayList<>()));
        }

        for (BlockEntityLiftingGasProvider heater : heaters) {
            BlockPos castPos = heater.getCastPosition();
            if (castPos == null) {
                continue;
            }
            for (BalloonGroup group : groups) {
                if (isAdjacent(group.envelopes(), castPos)) {
                    group.heaters().add(heater);
                    break;
                }
            }
        }
        return groups;
    }

    private static int choosePrimaryGroup(Balloon balloon, List<BalloonGroup> groups) {
        BlockPos controllerPos = balloon.getControllerPos();
        for (int i = 0; i < groups.size(); i++) {
            if (isAdjacent(groups.get(i).envelopes(), controllerPos)) {
                return i;
            }
        }
        for (int i = 0; i < groups.size(); i++) {
            if (!groups.get(i).heaters().isEmpty()) {
                return i;
            }
        }
        return 0;
    }

    private static BlockPos chooseController(Set<BlockPos> envelopes,
            List<BlockEntityLiftingGasProvider> heaters, BlockPos preferredController) {
        if (preferredController != null && isAdjacent(envelopes, preferredController)) {
            return preferredController;
        }
        for (BlockEntityLiftingGasProvider heater : heaters) {
            BlockPos castPos = heater.getCastPosition();
            if (castPos != null && isAdjacent(envelopes, castPos)) {
                return castPos;
            }
        }
        return envelopes.iterator().next();
    }

    private static BalloonLayerGraph buildGraph(Set<BlockPos> envelopes, BlockPos controllerPos) {
        Map<Integer, BalloonLayerData> layers = new TreeMap<>();
        Set<BlockPos> anchors = new LinkedHashSet<>();
        for (BlockPos pos : envelopes) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!envelopes.contains(neighbor)) {
                    anchors.add(neighbor);
                }
            }
        }

        for (BlockPos pos : envelopes) {
            addGraphPosition(layers, pos, false);
        }
        for (BlockPos anchor : anchors) {
            addGraphPosition(layers, anchor, true);
        }

        BalloonLayerGraph graph = new BalloonLayerGraph(controllerPos.getY());
        for (Map.Entry<Integer, BalloonLayerData> entry : layers.entrySet()) {
            graph.addLayer(entry.getKey(), entry.getValue());
        }
        graph.rebuildConnections(controllerPos);
        return graph;
    }

    private static void addGraphPosition(Map<Integer, BalloonLayerData> layers, BlockPos pos,
            boolean virtualAnchor) {
        BalloonLayerData layer = layers.computeIfAbsent(pos.getY(), BalloonLayerData::new);
        layer.hotAirCount++;
        layer.addHotAirBlock(pos.getX(), pos.getZ());
        if (virtualAnchor) {
            layer.solidCount++;
            layer.addSolidBlock(pos.getX(), pos.getZ());
        }
    }

    private static Set<BlockPos> collectEnvelopeBlocks(Balloon balloon) {
        Level level = getLevel(balloon);
        Set<BlockPos> envelopes = new LinkedHashSet<>();
        for (List<BalloonLayerData> layers : balloon.getGraph().getAllLayers()) {
            for (BalloonLayerData layer : layers) {
                Iterator<BlockPos> iterator = layer.nonSolidBlockIterator();
                while (iterator.hasNext()) {
                    BlockPos pos = iterator.next();
                    if (isEnvelope(level, pos)) {
                        envelopes.add(pos.immutable());
                    }
                }
            }
        }
        return envelopes;
    }

    private static Set<BlockPos> findConnectedEnvelopes(Level level, BlockPos origin) {
        BlockPos seed = findSeed(level, origin);
        if (seed == null) {
            return Set.of();
        }

        Set<BlockPos> envelopes = new LinkedHashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(seed);
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            if (!isEnvelope(level, current) || !envelopes.add(current)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!envelopes.contains(neighbor) && isEnvelope(level, neighbor)) {
                    frontier.addLast(neighbor);
                }
            }
        }
        return envelopes;
    }

    private static BlockPos findSeed(Level level, BlockPos origin) {
        if (isEnvelope(level, origin)) {
            return origin;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = origin.relative(direction);
            if (isEnvelope(level, neighbor)) {
                return neighbor;
            }
        }
        return null;
    }

    private static List<Set<BlockPos>> connectedComponents(Set<BlockPos> positions) {
        Set<BlockPos> remaining = new HashSet<>(positions);
        List<Set<BlockPos>> components = new ArrayList<>();

        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            remaining.remove(seed);
            Set<BlockPos> component = new LinkedHashSet<>();
            ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
            frontier.add(seed);

            while (!frontier.isEmpty()) {
                BlockPos current = frontier.removeFirst();
                component.add(current);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (remaining.remove(neighbor)) {
                        frontier.addLast(neighbor);
                    }
                }
            }
            components.add(component);
        }

        components.sort(Comparator.comparingLong(EnvelopeBalloonLogic::minimumPosition));
        return components;
    }

    private static long minimumPosition(Set<BlockPos> positions) {
        long minimum = Long.MAX_VALUE;
        for (BlockPos pos : positions) {
            minimum = Math.min(minimum, pos.asLong());
        }
        return minimum;
    }

    private static boolean isAdjacent(Set<BlockPos> envelopes, BlockPos pos) {
        if (envelopes.contains(pos)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (envelopes.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnvelope(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof EnvelopeBlock;
    }

    private static Level getLevel(Balloon balloon) {
        return ((BalloonStateAccessor) balloon).aavp$getLevel();
    }

    private record BalloonGroup(Set<BlockPos> envelopes, List<BlockEntityLiftingGasProvider> heaters) {
    }
}
