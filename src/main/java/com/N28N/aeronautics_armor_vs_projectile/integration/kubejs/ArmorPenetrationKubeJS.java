package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs;

import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;
import com.N28N.aeronautics_armor_vs_projectile.Config;

/**
 * KubeJS binding ({@code AAVP}) exposing the armor penetration attribute tables.
 */
public final class ArmorPenetrationKubeJS {
    public void registerProjectile(String entityTypeId, int penetrationLevel) {
        ArmorPenetrationRegistry.registerProjectile(entityTypeId, penetrationLevel);
    }

    public void registerBlock(String blockId, int protectionLevel) {
        ArmorPenetrationRegistry.registerBlock(blockId, protectionLevel);
    }

    public void setBlockToughness(String blockId, double toughness) {
        ArmorPenetrationRegistry.setBlockToughness(blockId, toughness);
    }

    public boolean hasBlockToughnessOverride(String blockId) {
        return ArmorPenetrationRegistry.hasBlockToughnessOverride(blockId);
    }

    public boolean resetBlockToughness(String blockId) {
        return ArmorPenetrationRegistry.removeBlockToughness(blockId);
    }

    public void setBlockHardness(String blockId, double hardness) {
        ArmorPenetrationRegistry.setBlockHardness(blockId, hardness);
    }

    public boolean hasBlockHardnessOverride(String blockId) {
        return ArmorPenetrationRegistry.hasBlockHardnessOverride(blockId);
    }

    public boolean resetBlockHardness(String blockId) {
        return ArmorPenetrationRegistry.removeBlockHardness(blockId);
    }

    public void setProjectileMassMultiplier(String entityTypeId, float multiplier) {
        ArmorPenetrationRegistry.setProjectileMassMultiplier(entityTypeId, multiplier);
    }

    public float getProjectileMassMultiplier(String entityTypeId) {
        return ArmorPenetrationRegistry.getProjectileMassMultiplier(entityTypeId);
    }

    public boolean resetProjectileMassMultiplier(String entityTypeId) {
        return ArmorPenetrationRegistry.removeProjectileMassMultiplier(entityTypeId);
    }

    public void setProjectileBonusMomentumMultiplier(String entityTypeId, float multiplier) {
        ArmorPenetrationRegistry.setProjectileBonusMomentumMultiplier(entityTypeId, multiplier);
    }

    public float getProjectileBonusMomentumMultiplier(String entityTypeId) {
        return ArmorPenetrationRegistry.getProjectileBonusMomentumMultiplier(entityTypeId);
    }

    public boolean resetProjectileBonusMomentumMultiplier(String entityTypeId) {
        return ArmorPenetrationRegistry.removeProjectileBonusMomentumMultiplier(entityTypeId);
    }

    public void setGlobalDamageScale(double scale) {
        Config.GLOBAL_DAMAGE_SCALE.set(scale);
    }

    public double getGlobalDamageScale() {
        return Config.GLOBAL_DAMAGE_SCALE.get();
    }

    public int getProjectileLevel(String entityTypeId) {
        return ArmorPenetrationRegistry.getRegisteredProjectileLevel(entityTypeId);
    }

    public int getBlockLevel(String blockId) {
        return ArmorPenetrationRegistry.getRegisteredBlockLevel(blockId);
    }

    public boolean isDebugLogging() {
        return Config.DEBUG_LOGGING.get();
    }
}

