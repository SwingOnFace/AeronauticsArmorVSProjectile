package com.N28N.aeronautics_armor_vs_projectile.mixin;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = ServerBalloon.class, remap = false)
public interface ServerBalloonGasAccessor {
    @Accessor("gasAmounts")
    Map<LiftingGasType, LiftingGasData> aavp$getGasAmounts();
}
