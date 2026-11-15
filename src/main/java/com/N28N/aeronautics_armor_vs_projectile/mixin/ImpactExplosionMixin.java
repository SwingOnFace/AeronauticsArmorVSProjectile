package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.N28N.aeronautics_armor_vs_projectile.AeronauticsArmorVSProjectile;
import com.N28N.aeronautics_armor_vs_projectile.Config;
import com.N28N.aeronautics_armor_vs_projectile.utils.PenetrationCombat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import rbasamoyai.createbigcannons.munitions.ImpactExplosion;

@Mixin(value = ImpactExplosion.class, remap = false)
public abstract class ImpactExplosionMixin {
    @Inject(method = "editBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$preventBlockEditsAfterStoppedImpact(Level level, BlockPos pos, BlockState state,
            FluidState fluidState, float power, CallbackInfo callbackInfo) {
        if (!PenetrationCombat.shouldSuppressBigCannonImpactBlockEdits()) {
            return;
        }
        if (Config.DEBUG_LOGGING.get()) {
            AeronauticsArmorVSProjectile.LOGGER.info(
                    "Suppressed CBC impact explosion block edit after penetration STOPPED: block={} pos={} power={}",
                    state.getBlock(), pos, power);
        }
        callbackInfo.cancel();
    }
}
