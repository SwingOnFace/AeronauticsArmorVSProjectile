package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;
import com.N28N.aeronautics_armor_vs_projectile.utils.PenetrationCombat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import rbasamoyai.createbigcannons.base.PartialBlockDamageManager;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;

@Mixin(value = AbstractAutocannonProjectile.class, remap = false)
public abstract class AutocannonProjectileMixin {
    @ModifyVariable(method = "calculateBlockPenetration", at = @At("STORE"), name = "mass", remap = false)
    private double aavp$scaleMass(double original) {
        return ArmorPenetrationRegistry.applyMassMultiplier((AbstractAutocannonProjectile) (Object) this, original);
    }

    @ModifyVariable(method = "calculateBlockPenetration", at = @At("STORE"), name = "bonusMomentum", remap = false)
    private double aavp$scaleBonusMomentum(double original) {
        return ArmorPenetrationRegistry.applyBonusMomentumMultiplier(
                (AbstractAutocannonProjectile) (Object) this, original);
    }

    @ModifyExpressionValue(
            method = "calculateBlockPenetration",
            at = @At(
                    value = "INVOKE",
                    target = "Lrbasamoyai/createbigcannons/block_armor_properties/BlockArmorPropertiesProvider;hardness(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Z)D"),
            remap = false)
    private double aavp$overrideHardness(double original,
            @Local(argsOnly = true) BlockState state,
            @Local(argsOnly = true) BlockHitResult blockHitResult) {
        AbstractAutocannonProjectile projectile = (AbstractAutocannonProjectile) (Object) this;
        return ArmorPenetrationRegistry.getBlockHardness(
                projectile.level(), blockHitResult.getBlockPos(), state);
    }

    @WrapOperation(
            method = "calculateBlockPenetration",
            at = @At(
                    value = "INVOKE",
                    target = "Lrbasamoyai/createbigcannons/base/PartialBlockDamageManager;damageBlock(Lnet/minecraft/core/BlockPos;FLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;)Z"),
            remap = false)
    private boolean aavp$replacePartialDamageWithPenetrationRoll(PartialBlockDamageManager damageManager,
            BlockPos pos, float rawDamage, BlockState state, Level level, Operation<Boolean> original) {
        AbstractAutocannonProjectile projectile = (AbstractAutocannonProjectile) (Object) this;
        if (PenetrationCombat.tryPenetrate(level, projectile, state, pos, rawDamage, "autocannon_impact")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }
}
