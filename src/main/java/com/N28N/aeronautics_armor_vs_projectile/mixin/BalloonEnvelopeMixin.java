package com.N28N.aeronautics_armor_vs_projectile.mixin;

import com.N28N.aeronautics_armor_vs_projectile.utils.EnvelopeBalloonLogic;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Balloon.class, remap = false)
public abstract class BalloonEnvelopeMixin {
    @Inject(method = "rebuildBalloonFromController", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$rebuildEnvelopeBalloon(CallbackInfo callbackInfo) {
        EnvelopeBalloonLogic.rebuildFromController((Balloon) (Object) this);
        callbackInfo.cancel();
    }

    @Inject(method = "onAirtightBlockAdded", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$onEnvelopeAdded(BlockPos pos, CallbackInfo callbackInfo) {
        EnvelopeBalloonLogic.onEnvelopeAdded((Balloon) (Object) this, pos);
        callbackInfo.cancel();
    }

    @Inject(method = "onAirtightBlockRemoved", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$onEnvelopeRemoved(BlockPos pos, CallbackInfo callbackInfo) {
        EnvelopeBalloonLogic.onEnvelopeRemoved((Balloon) (Object) this, pos);
        callbackInfo.cancel();
    }

    @Inject(method = "onSolidBlockAdded", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$ignoreSolidBlockAdded(BlockPos pos, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Inject(method = "onSolidBlockRemoved", at = @At("HEAD"), cancellable = true, remap = false)
    private void aavp$ignoreSolidBlockRemoved(BlockPos pos, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

}
