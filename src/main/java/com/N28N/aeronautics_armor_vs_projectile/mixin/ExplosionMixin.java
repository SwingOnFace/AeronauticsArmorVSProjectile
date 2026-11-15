package com.N28N.aeronautics_armor_vs_projectile.mixin;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.N28N.aeronautics_armor_vs_projectile.AeronauticsArmorVSProjectile;
import com.N28N.aeronautics_armor_vs_projectile.Config;
import com.N28N.aeronautics_armor_vs_projectile.utils.PenetrationCombat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(value = Explosion.class, remap = false)
public abstract class ExplosionMixin {
    @Shadow(remap = false)
    @Final
    private Level level;

    @Shadow(remap = false)
    @Final
    private float radius;

    @Shadow(remap = false)
    @Final
    private Entity source;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"), remap = false)
    private void aavp$filterBlocksByPenetrationChance(boolean spawnParticles, CallbackInfo callbackInfo) {
        if (this.level.isClientSide || !(this.source instanceof AbstractCannonProjectile projectile)) {
            return;
        }

        Explosion explosion = (Explosion) (Object) this;
        if (!explosion.interactsWithBlocks() || this.radius <= 0) {
            return;
        }

        List<BlockPos> blocks = explosion.getToBlow();
        int candidateCount = blocks.size();
        int filteredCount = 0;
        Vec3 center = explosion.center();
        Iterator<BlockPos> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = this.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            double distance = Math.sqrt(pos.getCenter().distanceToSqr(center));
            float attenuation = (float) Math.max(0, 1 - distance / (this.radius * 2));
            float rawDamage = this.radius * attenuation;
            if (!PenetrationCombat.tryPenetrate(
                    this.level, projectile, state, pos, rawDamage, "explosion")) {
                iterator.remove();
                filteredCount++;
            }
        }
        if (Config.DEBUG_LOGGING.get()) {
            AeronauticsArmorVSProjectile.LOGGER.info(
                    "Explosion block filter: projectile={} center={} radius={} candidates={} filtered={} retained={}",
                    projectile.getType(), center, this.radius, candidateCount, filteredCount,
                    candidateCount - filteredCount);
        }
    }
}
