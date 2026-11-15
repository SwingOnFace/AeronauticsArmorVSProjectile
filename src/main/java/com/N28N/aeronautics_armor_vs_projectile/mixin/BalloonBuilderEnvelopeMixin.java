package com.N28N.aeronautics_armor_vs_projectile.mixin;

import com.N28N.aeronautics_armor_vs_projectile.utils.EnvelopeBalloonLogic;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BalloonBuilder.class, remap = false)
public abstract class BalloonBuilderEnvelopeMixin {
    @Inject(method = "attemptBuildBalloon", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aavp$buildEnvelopeBalloon(BlockEntityLiftingGasProvider heater, BlockPos startPos,
            CallbackInfoReturnable<Balloon> callbackInfo) {
        callbackInfo.setReturnValue(EnvelopeBalloonLogic.createBalloon(heater, startPos));
    }
}
