package com.N28N.aeronautics_armor_vs_projectile.compat;

import java.lang.reflect.Method;

import net.minecraft.world.entity.Entity;

/**
 * Soft compatibility with CBC More Shells (cbcmoreshells). The mod is optional and never referenced at
 * compile time, so its projectile class is resolved by name; if it is missing every query reports
 * "not a dual cannon shell".
 */
public final class DualCannonCompat {
    private static final String PROJECTILE_CLASS_NAME =
            "com.cainiao1053.cbcmoreshells.munitions.dual_cannon.AbstractDualCannonProjectile";
    private static final String DURABILITY_MODIFIER_METHOD = "getDurabilityModifier";

    private static final Class<?> PROJECTILE_CLASS = resolveProjectileClass();
    private static final Method DURABILITY_MODIFIER = resolveDurabilityModifier();

    private DualCannonCompat() {
    }

    public static boolean isDualCannonProjectile(Entity entity) {
        return PROJECTILE_CLASS != null && PROJECTILE_CLASS.isInstance(entity);
    }

    public static float getDurabilityModifier(Entity entity) {
        if (DURABILITY_MODIFIER == null || !isDualCannonProjectile(entity)) {
            return 0.0F;
        }
        try {
            return (Float) DURABILITY_MODIFIER.invoke(entity);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return 0.0F;
        }
    }

    private static Class<?> resolveProjectileClass() {
        try {
            return Class.forName(PROJECTILE_CLASS_NAME, false, DualCannonCompat.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError absent) {
            return null;
        }
    }

    private static Method resolveDurabilityModifier() {
        if (PROJECTILE_CLASS == null) {
            return null;
        }
        try {
            Method method = PROJECTILE_CLASS.getMethod(DURABILITY_MODIFIER_METHOD);
            method.trySetAccessible();
            return method;
        } catch (NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }
}
