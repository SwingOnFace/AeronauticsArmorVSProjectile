package com.N28N.aeronautics_armor_vs_projectile.utils;

import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;
import com.N28N.aeronautics_armor_vs_projectile.AeronauticsArmorVSProjectile;
import com.N28N.aeronautics_armor_vs_projectile.Config;
import com.N28N.aeronautics_armor_vs_projectile.api.ProjectilePenetrationData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;

public final class PenetrationCombat {
    private static final ThreadLocal<Boolean> BIG_CANNON_IMPACT_STOPPED = ThreadLocal.withInitial(() -> false);

    private PenetrationCombat() {
    }

    public static void beginBigCannonImpact() {
        BIG_CANNON_IMPACT_STOPPED.set(false);
    }

    public static void endBigCannonImpact() {
        BIG_CANNON_IMPACT_STOPPED.remove();
    }

    public static boolean shouldSuppressBigCannonImpactBlockEdits() {
        return BIG_CANNON_IMPACT_STOPPED.get();
    }

    public static boolean tryPenetrate(Level level, ProjectileContext projectileContext, BlockState state, BlockPos pos,
            float rawDamage, String attackSource) 
    {
        return tryPenetrate(level, projectileContext.projectile, state, pos, rawDamage, attackSource);
    }

    public static boolean tryPenetrate(Level level, Entity projectile, BlockState state, BlockPos pos,
            float rawDamage, String attackSource) 
    {
        boolean debugLogging = Config.DEBUG_LOGGING.get();
        if (level.isClientSide) {
            if (debugLogging) {
                AeronauticsArmorVSProjectile.LOGGER.info(
                        "Penetration check skipped on client: source={} projectile={} block={} pos={} rawDamage={}",
                        attackSource,
                        BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()),
                        BuiltInRegistries.BLOCK.getKey(state.getBlock()), pos, rawDamage);
            }
            return false;
        }
        int entityPenetrationLevel = projectile instanceof ProjectilePenetrationData data
                ? data.aavp$getPenetrationLevel()
                : 0;
        boolean hasProjectileLevel = entityPenetrationLevel > 0;
        int pierceLevel = hasProjectileLevel
                ? entityPenetrationLevel
                : ArmorPenetrationRegistry.getPierceLevel(projectile);
        int protectionLevel = ArmorPenetrationRegistry.getProtectionLevel(level, pos, state);
        float globalDamageScale = Config.GLOBAL_DAMAGE_SCALE.get().floatValue();
        float scaledRawDamage = rawDamage * globalDamageScale;
        int levelDisadvantage = Math.max(protectionLevel - pierceLevel, 0);
        float damageMultiplier = Math.scalb(1.0f, -levelDisadvantage);
        float effectiveDamage = DamageCalculator.solveDamage(scaledRawDamage, pierceLevel, protectionLevel);
        double durability = ArmorPenetrationRegistry.getDurability(level, pos, state);
        float chance = DamageCalculator.penetrationChance(effectiveDamage, durability);
        float roll = level.getRandom().nextFloat();
        boolean penetrated = roll < chance;
        if ("big_cannon_impact".equals(attackSource) && !penetrated) {
            BIG_CANNON_IMPACT_STOPPED.set(true);
        }

        if (debugLogging) {
            AeronauticsArmorVSProjectile.LOGGER.info(
                    "Penetration input: source={} projectile={} pierceLevel={} pierceLevelSource={} block={} pos={} protectionLevel={} rawDamage={} globalDamageScale={} scaledRawDamage={}",
                    attackSource,
                    BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()),
                    pierceLevel, hasProjectileLevel ? "projectile entity data" : "entity type registry/config",
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()), pos, protectionLevel,
                    rawDamage, globalDamageScale, scaledRawDamage);
            String chanceFormula = effectiveDamage == 0
                    ? "effectiveDamage=0 => 0"
                    : durability <= 0
                            ? "durability<=0 => 1"
                            : "clamp(effectiveDamage / durability, 0, 1)";
            AeronauticsArmorVSProjectile.LOGGER.info(
                    "Penetration result: source={} effectiveDamage={} = rawDamage({}) * globalDamageScale({}) * levelMultiplier({}); levelMultiplier=2^-max(protectionLevel({})-pierceLevel({}), 0)=2^-{}={}; toughness={} => penetrationChance={} ({}, percent={}); randomRoll={} => {}",
                    attackSource, effectiveDamage, rawDamage, globalDamageScale,
                    damageMultiplier, protectionLevel, pierceLevel,
                    levelDisadvantage, damageMultiplier, durability, chance, chanceFormula, chance * 100,
                    roll, penetrated ? "PENETRATED" : "STOPPED");
        }
        return penetrated;
    }
}
