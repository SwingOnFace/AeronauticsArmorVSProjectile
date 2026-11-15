package com.N28N.aeronautics_armor_vs_projectile;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROJECTILE_PIERCE_LEVELS =
            BUILDER.comment("Projectile entity IDs and penetration levels, formatted as namespace:id=level.")
                    .defineListAllowEmpty("projectilePierceLevels", List.of(), () -> "", Config::isLevelEntry);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_PROTECTION_LEVELS =
            BUILDER.comment("Block IDs and protection levels, formatted as namespace:id=level.")
                    .defineListAllowEmpty("blockProtectionLevels", List.of(), () -> "", Config::isLevelEntry);

    public static final ModConfigSpec.DoubleValue TIER_2_TOUGHNESS =
            BUILDER.comment("CBC toughness required for protection level 2 (legacy key: tier2Hardness).")
                    .defineInRange("tier2Hardness", 4.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue TIER_3_TOUGHNESS =
            BUILDER.comment("CBC toughness required for protection level 3 (legacy key: tier3Hardness).")
                    .defineInRange("tier3Hardness", 8.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue TIER_4_TOUGHNESS =
            BUILDER.comment("CBC toughness required for protection level 4 (legacy key: tier4Hardness).")
                    .defineInRange("tier4Hardness", 2147483647.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue TIER_5_TOUGHNESS =
            BUILDER.comment("CBC toughness required for protection level 5 (legacy key: tier5Hardness).")
                    .defineInRange("tier5Hardness", 2147483647.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue TIER_6_TOUGHNESS =
            BUILDER.comment("CBC toughness required for protection level 6 (legacy key: tier6Hardness).")
                    .defineInRange("tier6Hardness", 2147483647.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue GLOBAL_DAMAGE_SCALE =
            BUILDER.comment("Global multiplier applied to raw penetration damage before penetration chance is computed.")
                    .defineInRange("globalDamageScale", 0.1, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING =
            BUILDER.comment("Log detailed armor penetration calculations.")
                    .define("debugLogging", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static boolean isLevelEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        int separator = entry.lastIndexOf('=');
        if (separator <= 0 || separator == entry.length() - 1
                || ResourceLocation.tryParse(entry.substring(0, separator)) == null) {
            return false;
        }
        try {
            int level = Integer.parseInt(entry.substring(separator + 1));
            return level >= 1 && level <= 6;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
