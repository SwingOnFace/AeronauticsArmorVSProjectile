package com.N28N.aeronautics_armor_vs_projectile.utils;

public final class DamageCalculator 
{
    private DamageCalculator() 
    {
        throw new AssertionError("No instances");
    }

    public static float solveDamage(float rawDamage, int pierceLevel, int protectLevel) {
        if (!Float.isFinite(rawDamage) || rawDamage < 0) {
            throw new IllegalArgumentException("rawDamage must be finite and non-negative");
        }
        validateLevel(pierceLevel, "pierceLevel");
        validateLevel(protectLevel, "protectLevel");

        int levelDisadvantage = protectLevel - pierceLevel;
        return levelDisadvantage > 0 ? Math.scalb(rawDamage, -levelDisadvantage) : rawDamage;
    }

    public static float penetrationChance(float effectiveDamage, double durability) {
        if (!Float.isFinite(effectiveDamage) || effectiveDamage < 0) {
            throw new IllegalArgumentException("effectiveDamage must be finite and non-negative");
        }
        if (Double.isNaN(durability)) {
            throw new IllegalArgumentException("durability must not be NaN");
        }
        if (effectiveDamage == 0) {
            return 0;
        }
        if (durability <= 0) {
            return 1;
        }
        return (float) Math.min(effectiveDamage / durability, 1);
    }

    private static void validateLevel(int level, String name) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException(name + " must be between 1 and 6");
        }
    }
}
