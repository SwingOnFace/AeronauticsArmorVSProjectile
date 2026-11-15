package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.N28N.aeronautics_armor_vs_projectile.api.ProjectilePenetrationData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;

@Mixin(value = BigCannonProjectileRenderer.class, remap = false)
public abstract class BigCannonProjectileRendererMixin {
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lrbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile;getRenderedBlockState()Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false)
    private BlockState aavp$useRegisteredProjectileModel(AbstractBigCannonProjectile projectile,
            Operation<BlockState> original) {
        if (projectile instanceof ProjectilePenetrationData data) {
            BlockState customState = data.aavp$getRenderedBlockState();
            if (customState != null) {
                return customState;
            }
        }
        return original.call(projectile);
    }
}
