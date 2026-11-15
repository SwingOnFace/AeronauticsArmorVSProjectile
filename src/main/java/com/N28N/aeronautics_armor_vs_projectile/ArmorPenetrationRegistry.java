package com.N28N.aeronautics_armor_vs_projectile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.N28N.aeronautics_armor_vs_projectile.compat.DualCannonCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBigCannonProjectile;

public final class ArmorPenetrationRegistry {
    private static final Map<EntityType<?>, Integer> REGISTERED_PROJECTILE_LEVELS = new ConcurrentHashMap<>();
    private static final Map<Block, Integer> REGISTERED_BLOCK_LEVELS = new ConcurrentHashMap<>();

    private static final Map<Block, Double> BLOCK_TOUGHNESS_OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<Block, Double> BLOCK_HARDNESS_OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, Float> PROJECTILE_MASS_MULTIPLIERS = new ConcurrentHashMap<>();
    private static final Map<EntityType<?>, Float> PROJECTILE_BONUS_MOMENTUM_MULTIPLIERS = new ConcurrentHashMap<>();

    private static volatile Map<EntityType<?>, Integer> configuredProjectileLevels = Map.of();
    private static volatile Map<Block, Integer> configuredBlockLevels = Map.of();

    private ArmorPenetrationRegistry() {
    }

    public static void registerProjectile(EntityType<?> projectileType, int pierceLevel) {
        REGISTERED_PROJECTILE_LEVELS.put(Objects.requireNonNull(projectileType), validateLevel(pierceLevel));
    }

    public static void registerBlock(Block block, int protectLevel) {
        REGISTERED_BLOCK_LEVELS.put(Objects.requireNonNull(block), validateLevel(protectLevel));
    }

    public static void setBlockToughness(Block block, double toughness) {
        BLOCK_TOUGHNESS_OVERRIDES.put(Objects.requireNonNull(block), validateToughness(toughness));
    }

    public static void setBlockToughness(String blockId, double toughness) {
        setBlockToughness(resolveBlock(blockId), toughness);
    }

    public static boolean hasBlockToughnessOverride(Block block) {
        return BLOCK_TOUGHNESS_OVERRIDES.containsKey(block);
    }

    public static boolean hasBlockToughnessOverride(String blockId) {
        return hasBlockToughnessOverride(resolveBlock(blockId));
    }

    public static int getRegisteredProjectileLevel(String entityTypeId) {
        EntityType<?> type = resolveProjectileType(entityTypeId);
        Integer registered = REGISTERED_PROJECTILE_LEVELS.get(type);
        if (registered != null) {
            return registered;
        }
        Integer configured = configuredProjectileLevels.get(type);
        return configured != null ? configured : 0;
    }

    public static int getRegisteredBlockLevel(String blockId) {
        Block block = resolveBlock(blockId);
        Integer level = REGISTERED_BLOCK_LEVELS.get(block);
        if (level != null) {
            return level;
        }
        Integer configured = configuredBlockLevels.get(block);
        return configured != null ? configured : 0;
    }

    public static boolean removeBlockToughness(String blockId) {
        return BLOCK_TOUGHNESS_OVERRIDES.remove(resolveBlock(blockId)) != null;
    }

    public static void setBlockHardness(Block block, double hardness) {
        BLOCK_HARDNESS_OVERRIDES.put(Objects.requireNonNull(block), validateNonNegative(hardness, "Block hardness"));
    }

    public static void setBlockHardness(String blockId, double hardness) {
        setBlockHardness(resolveBlock(blockId), hardness);
    }

    public static boolean hasBlockHardnessOverride(String blockId) {
        return BLOCK_HARDNESS_OVERRIDES.containsKey(resolveBlock(blockId));
    }

    public static boolean removeBlockHardness(String blockId) {
        return BLOCK_HARDNESS_OVERRIDES.remove(resolveBlock(blockId)) != null;
    }

    public static double getBlockHardness(Level level, BlockPos pos, BlockState state) {
        Double override = BLOCK_HARDNESS_OVERRIDES.get(state.getBlock());
        if (override != null) {
            return override;
        }
        return getBlockProperties(state).hardness(level, state, pos, true);
    }

    public static void setProjectileMassMultiplier(String entityTypeId, float multiplier) {
        PROJECTILE_MASS_MULTIPLIERS.put(resolveProjectileType(entityTypeId),
                validateMultiplier(multiplier, "Projectile mass multiplier"));
    }

    public static float getProjectileMassMultiplier(EntityType<?> projectileType) {
        return PROJECTILE_MASS_MULTIPLIERS.getOrDefault(projectileType, 1.0F);
    }

    public static float getProjectileMassMultiplier(String entityTypeId) {
        return getProjectileMassMultiplier(resolveProjectileType(entityTypeId));
    }

    public static boolean removeProjectileMassMultiplier(String entityTypeId) {
        return PROJECTILE_MASS_MULTIPLIERS.remove(resolveProjectileType(entityTypeId)) != null;
    }

    public static void setProjectileBonusMomentumMultiplier(String entityTypeId, float multiplier) {
        PROJECTILE_BONUS_MOMENTUM_MULTIPLIERS.put(resolveProjectileType(entityTypeId),
                validateMultiplier(multiplier, "Projectile bonus momentum multiplier"));
    }

    public static float getProjectileBonusMomentumMultiplier(EntityType<?> projectileType) {
        return PROJECTILE_BONUS_MOMENTUM_MULTIPLIERS.getOrDefault(projectileType, 1.0F);
    }

    public static float getProjectileBonusMomentumMultiplier(String entityTypeId) {
        return getProjectileBonusMomentumMultiplier(resolveProjectileType(entityTypeId));
    }

    public static boolean removeProjectileBonusMomentumMultiplier(String entityTypeId) {
        return PROJECTILE_BONUS_MOMENTUM_MULTIPLIERS.remove(resolveProjectileType(entityTypeId)) != null;
    }

    public static double applyMassMultiplier(Entity projectile, double mass) {
        return mass * getProjectileMassMultiplier(projectile.getType());
    }

    public static double applyBonusMomentumMultiplier(Entity projectile, double bonusMomentum) {
        return bonusMomentum * getProjectileBonusMomentumMultiplier(projectile.getType());
    }

    public static int getPierceLevel(Entity projectile) {
        EntityType<?> projectileType = projectile.getType();
        Integer registered = REGISTERED_PROJECTILE_LEVELS.get(projectileType);
        if (registered != null) {
            return registered;
        }
        Integer configured = configuredProjectileLevels.get(projectileType);
        if (configured != null) {
            return configured;
        }
        boolean isBigCannonProjectile = projectile instanceof FuzedBigCannonProjectile;
        int defaultLevel = isBigCannonProjectile ? 3 : 1;
        if (!isBigCannonProjectile
                && DualCannonCompat.isDualCannonProjectile(projectile)
                && DualCannonCompat.getDurabilityModifier(projectile) >= 1.5F) {
            defaultLevel++;
        }
        return defaultLevel;
    }

    public static void registerProjectile(String entityTypeId, int pierceLevel) {
        registerProjectile(resolveProjectileType(entityTypeId), pierceLevel);
    }

    public static void registerBlock(String blockId, int protectLevel) {
        registerBlock(resolveBlock(blockId), protectLevel);
    }

    public static int getProtectionLevel(Level level, BlockPos pos, BlockState state) {
        Integer registered = REGISTERED_BLOCK_LEVELS.get(state.getBlock());
        if (registered != null) {
            return registered;
        }
        Integer configured = configuredBlockLevels.get(state.getBlock());
        if (configured != null) {
            return configured;
        }

        double toughness = getDurability(level, pos, state);
        if (!Double.isFinite(toughness) || toughness < 0) {
            AeronauticsArmorVSProjectile.LOGGER.warn("Ignoring invalid CBC toughness {} for block {}", toughness,
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            return 1;
        }
        if (toughness >= Config.TIER_6_TOUGHNESS.get()) {
            return 6;
        }
        if (toughness >= Config.TIER_5_TOUGHNESS.get()) {
            return 5;
        }
        if (toughness >= Config.TIER_4_TOUGHNESS.get()) {
            return 4;
        }
        if (toughness >= Config.TIER_3_TOUGHNESS.get()) {
            return 3;
        }
        if (toughness >= Config.TIER_2_TOUGHNESS.get()) {
            return 2;
        }
        return 1;
    }

    public static double getDurability(Level level, BlockPos pos, BlockState state) {
        Double override = BLOCK_TOUGHNESS_OVERRIDES.get(state.getBlock());
        if (override != null) {
            return override;
        }
        return getBlockProperties(state).toughness(level, state, pos, true);
    }

    static void reloadConfiguredLevels() {
        double tier2Toughness = Config.TIER_2_TOUGHNESS.get();
        double tier3Toughness = Config.TIER_3_TOUGHNESS.get();
        double[] thresholds = {
                tier2Toughness,
                tier3Toughness,
                Config.TIER_4_TOUGHNESS.get(),
                Config.TIER_5_TOUGHNESS.get(),
                Config.TIER_6_TOUGHNESS.get()
        };
        for (int index = 1; index < thresholds.length; index++) {
            if (thresholds[index] < thresholds[index - 1]) {
                throw new IllegalStateException("CBC toughness thresholds must be non-decreasing from tier2Hardness to tier6Hardness");
            }
        }
        configuredProjectileLevels = parseProjectileLevels(Config.PROJECTILE_PIERCE_LEVELS.get());
        configuredBlockLevels = parseBlockLevels(Config.BLOCK_PROTECTION_LEVELS.get());
    }

    private static Map<EntityType<?>, Integer> parseProjectileLevels(List<? extends String> entries) {
        Map<EntityType<?>, Integer> result = new HashMap<>();
        for (String entry : entries) {
            ParsedLevel parsed = parseEntry(entry);
            if (parsed == null) {
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(parsed.id())) {
                AeronauticsArmorVSProjectile.LOGGER.warn("Ignoring unknown projectile entity type in config: {}", parsed.id());
                continue;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(parsed.id());
            if (result.put(type, parsed.level()) != null) {
                AeronauticsArmorVSProjectile.LOGGER.warn("Duplicate projectile level config for {}; using the last entry", parsed.id());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<Block, Integer> parseBlockLevels(List<? extends String> entries) {
        Map<Block, Integer> result = new HashMap<>();
        for (String entry : entries) {
            ParsedLevel parsed = parseEntry(entry);
            if (parsed == null) {
                continue;
            }
            if (!BuiltInRegistries.BLOCK.containsKey(parsed.id())) {
                AeronauticsArmorVSProjectile.LOGGER.warn("Ignoring unknown block in config: {}", parsed.id());
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.get(parsed.id());
            if (result.put(block, parsed.level()) != null) {
                AeronauticsArmorVSProjectile.LOGGER.warn("Duplicate block protection config for {}; using the last entry", parsed.id());
            }
        }
        return Map.copyOf(result);
    }

    private static ParsedLevel parseEntry(String entry) {
        int separator = entry.lastIndexOf('=');
        ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, separator));
        int level = Integer.parseInt(entry.substring(separator + 1));
        return new ParsedLevel(id, level);
    }

    private static int validateLevel(int level) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Armor levels must be between 1 and 6");
        }
        return level;
    }

    private static float validateMultiplier(float multiplier, String name) {
        if (!Float.isFinite(multiplier) || multiplier < 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + multiplier);
        }
        return multiplier;
    }

    private static double validateToughness(double toughness) {
        return validateNonNegative(toughness, "Block toughness");
    }

    private static double validateNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
        return value;
    }

    private static EntityType<?> resolveProjectileType(String entityTypeId) {
        ResourceLocation id = parseId(entityTypeId, "projectile entity type");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            throw new IllegalArgumentException("Unknown projectile entity type: " + id);
        }
        return type;
    }

    private static Block resolveBlock(String blockId) {
        ResourceLocation id = parseId(blockId, "block");
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalArgumentException("Unknown block: " + id);
        }
        return BuiltInRegistries.BLOCK.get(id);
    }

    private static ResourceLocation parseId(String value, String kind) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid " + kind + " ID: " + value);
        }
        return id;
    }

    private static BlockArmorPropertiesProvider getBlockProperties(BlockState state) {
        return BlockArmorPropertiesHandler.getProperties(state);
    }

    private record ParsedLevel(ResourceLocation id, int level) {
    }
}
