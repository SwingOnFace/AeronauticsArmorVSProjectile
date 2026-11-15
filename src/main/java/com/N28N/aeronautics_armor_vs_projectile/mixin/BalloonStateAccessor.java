package com.N28N.aeronautics_armor_vs_projectile.mixin;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(value = Balloon.class, remap = false)
public interface BalloonStateAccessor {
    @Accessor("level")
    Level aavp$getLevel();

    @Accessor("graph")
    void aavp$setGraph(BalloonLayerGraph graph);

    @Accessor("controllerPos")
    void aavp$setControllerPos(BlockPos controllerPos);

    @Accessor("capacity")
    void aavp$setCapacity(int capacity);

    @Accessor("heaters")
    Set<BlockEntityLiftingGasProvider> aavp$getHeaters();

    @Invoker("recomputeBalloonData")
    void aavp$recomputeBalloonData();

    @Invoker("onRebuilt")
    void aavp$onRebuilt();
}
