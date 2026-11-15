package com.N28N.aeronautics_armor_vs_projectile.mixin;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.HeatedCulledRenderRegion;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeBlock;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.NoSuchElementException;

@Mixin(value = HeatedCulledRenderRegion.class, remap = false)
public abstract class HeatedCulledRenderRegionMixin {
    @Shadow
    @Final
    private LevelAccelerator accelerator;

    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/eriksonn/aeronautics/content/blocks/hot_air/balloon/graph/BalloonLayerData;blockIterator()Ljava/util/Iterator;"
            ),
            remap = false
    )
    private Iterator<BlockPos> aavp$renderOnlyEnvelopes(BalloonLayerData layer) {
        Iterator<BlockPos> blocks = layer.blockIterator();
        return new Iterator<>() {
            private BlockPos next;

            @Override
            public boolean hasNext() {
                while (this.next == null && blocks.hasNext()) {
                    BlockPos candidate = blocks.next();
                    if (HeatedCulledRenderRegionMixin.this.accelerator.getBlockState(candidate)
                            .getBlock() instanceof EnvelopeBlock) {
                        this.next = candidate.immutable();
                    }
                }
                return this.next != null;
            }

            @Override
            public BlockPos next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                BlockPos result = this.next;
                this.next = null;
                return result;
            }
        };
    }
}
