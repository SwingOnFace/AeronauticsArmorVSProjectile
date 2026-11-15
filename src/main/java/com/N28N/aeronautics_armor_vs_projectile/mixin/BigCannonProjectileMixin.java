package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.N28N.aeronautics_armor_vs_projectile.AeronauticsArmorVSProjectile;
import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;
import com.N28N.aeronautics_armor_vs_projectile.Config;
import com.N28N.aeronautics_armor_vs_projectile.utils.PenetrationCombat;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions.GriefState;
import rbasamoyai.createbigcannons.munitions.ImpactExplosion;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.ProjectileDamageHooks;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult.KinematicOutcome;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.network.ClientboundPlayBlockHitEffectPacket;
import rbasamoyai.createbigcannons.utils.CBCUtils;

@Mixin(value = AbstractBigCannonProjectile.class, remap = false)
public abstract class BigCannonProjectileMixin {
    /**
     * Replaces CBC's penetration handling so its block removal uses the armor penetration roll.
     */
    @Overwrite(remap = false)
    protected ImpactResult calculateBlockPenetration(
            ProjectileContext projectileContext, BlockState state, BlockHitResult blockHitResult) {
        AbstractBigCannonProjectile projectile = (AbstractBigCannonProjectile) (Object) this;
        AbstractCannonProjectileAccess access = (AbstractCannonProjectileAccess) projectile;
        PenetrationCombat.beginBigCannonImpact();
        try {
            BlockPos pos = blockHitResult.getBlockPos();
            Vec3 hitLoc = blockHitResult.getLocation();
            BallisticPropertiesComponent ballistics = access.aavp$invokeGetBallisticProperties();
            BlockArmorPropertiesProvider blockArmor = BlockArmorPropertiesHandler.getProperties(state);
            boolean unbreakable = projectileContext.griefState() == GriefState.NO_DAMAGE
                    || state.getDestroySpeed(projectile.level(), pos) == -1.0F
                    || !ProjectileDamageHooks.canDamageTerrain(projectile.level(), pos);
            Vec3 accel = access.aavp$invokeGetForces(projectile.position(), projectile.getDeltaMovement());
            Vec3 curVel = projectile.getDeltaMovement().add(accel);
            Vec3 normal = CBCUtils.getSurfaceNormalVector(projectile.level(), blockHitResult);
            double incidence = Math.max(0.0, curVel.normalize().dot(normal.reverse()));
            double velMag = curVel.length();
            double baseMass = projectile.getProjectileMass();
            double mass = ArmorPenetrationRegistry.applyMassMultiplier(projectile, baseMass);
            double bonusMomentum = ArmorPenetrationRegistry.applyBonusMomentumMultiplier(projectile,
                    1.0 + Math.max(0.0,
                            (velMag - CBCConfigs.server().munitions.minVelocityForPenetrationBonus.getF())
                                    * CBCConfigs.server().munitions.penetrationBonusScale.getF()));
            double incidentVel = velMag * incidence;
            double momentum = mass * incidentVel * bonusMomentum;
            double toughness = blockArmor.toughness(projectile.level(), state, pos, true);
            double toughnessPenalty = toughness - momentum;
            double hardnessPenalty = ArmorPenetrationRegistry.getBlockHardness(projectile.level(), pos, state)
                    - ballistics.penetration();
            double bounceBonus = Math.max(1.0 - hardnessPenalty, 0.0);
            double projectileDeflection = ballistics.deflection();
            double baseChance = CBCConfigs.server().munitions.baseProjectileBounceChance.getF();
            double bounceChance = projectileDeflection >= 0.01 && incidence <= projectileDeflection
                    ? Math.max(baseChance, 1.0 - incidence / projectileDeflection) * bounceBonus
                    : 0.0;
            boolean surfaceImpact = access.aavp$invokeCanHitSurface();
            boolean canBounce = CBCConfigs.server().munitions.projectilesCanBounce.get();
            boolean cbcBlockBroken = toughnessPenalty < 0.01 && !unbreakable;
            boolean blockBroken = !unbreakable && PenetrationCombat.tryPenetrate(
                    projectile.level(), projectileContext, state, pos, (float) momentum, "big_cannon_impact");

            KinematicOutcome outcome;
            if (surfaceImpact && canBounce && projectile.level().getRandom().nextDouble() < bounceChance) {
                outcome = KinematicOutcome.BOUNCE;
            } else if (blockBroken && !projectile.level().isClientSide) {
                outcome = KinematicOutcome.PENETRATE;
            } else {
                outcome = KinematicOutcome.STOP;
            }

            boolean shatter = surfaceImpact && outcome != KinematicOutcome.BOUNCE
                    && hardnessPenalty > ballistics.toughness();
            float durabilityPenalty = ((float) Math.max(0.0, hardnessPenalty) + 1.0F)
                    * (float) toughness / (float) incidentVel;
            state.onProjectileHit(projectile.level(), state, blockHitResult, projectile);
            if (!projectile.level().isClientSide) {
                boolean bounced = outcome == KinematicOutcome.BOUNCE;
                Vec3 effectNormal = bounced
                        ? curVel.subtract(normal.scale(normal.dot(curVel) * 1.7))
                        : curVel.reverse();
                for (BlockState containedState : blockArmor.containedBlockStates(
                        projectile.level(), state, pos.immutable(), true)) {
                    projectileContext.addPlayedEffect(new ClientboundPlayBlockHitEffectPacket(
                            containedState, projectile.getType(), bounced, true,
                            hitLoc.x, hitLoc.y, hitLoc.z,
                            (float) effectNormal.x, (float) effectNormal.y, (float) effectNormal.z));
                }
            }

            if (Config.DEBUG_LOGGING.get()) {
                AeronauticsArmorVSProjectile.LOGGER.info(
                        "Big cannon impact decision: block={} pos={} cbcBlockBroken={} penetrationBlockBroken={} outcome={} unbreakable={} toughnessPenalty={} momentum={}",
                        state.getBlock(), pos, cbcBlockBroken, blockBroken, outcome, unbreakable,
                        toughnessPenalty, momentum);
            }

            if (blockBroken) {
                projectile.setProjectileMass(incidentVel < 1.0E-4
                        ? 0.0F
                        : Math.max(projectile.getProjectileMass() - durabilityPenalty, 0.0F));
                projectile.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                if (surfaceImpact) {
                    float f = (float) toughness / (float) momentum;
                    float overPenetrationPower = f < 0.15F ? 2.0F - 2.0F * f : 0.0F;
                    if (overPenetrationPower > 0.0F && outcome == KinematicOutcome.PENETRATE) {
                        projectileContext.queueExplosion(pos, overPenetrationPower);
                    }
                }
            } else {
                if (outcome == KinematicOutcome.STOP) {
                    projectile.setProjectileMass(0.0F);
                    if (!projectile.level().isClientSide) {
                        access.aavp$setImpactPos(blockHitResult.getBlockPos());
                    }
                } else {
                    projectile.setProjectileMass(incidentVel < 1.0E-4
                            ? 0.0F
                            : Math.max(projectile.getProjectileMass() - durabilityPenalty / 2.0F, 0.0F));
                }

                Vec3 spallLoc = hitLoc.add(curVel.normalize().scale(2.0));
                if (!projectile.level().isClientSide) {
                    ImpactExplosion explosion = new ImpactExplosion(
                            projectile.level(), projectile, projectile.indirectArtilleryFire(false),
                            spallLoc.x, spallLoc.y, spallLoc.z, 2.0F, 2.0F, BlockInteraction.KEEP);
                    CreateBigCannons.handleCustomExplosion(projectile.level(), explosion);
                }

                SoundType sound = state.getSoundType(projectile.level(), pos, projectile);
                if (!projectile.level().isClientSide) {
                    projectile.level().playSound(
                            (Player) null, spallLoc.x, spallLoc.y, spallLoc.z,
                            sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
                }
            }

            access.aavp$setMassLost((float) (baseMass - (double) projectile.getProjectileMass())
                    / ballistics.durabilityMass());
            shatter |= access.aavp$invokeOnImpact(
                    blockHitResult, new ImpactResult(outcome, shatter), projectileContext);
            return new ImpactResult(outcome, shatter);
        } finally {
            PenetrationCombat.endBigCannonImpact();
        }
    }
}
