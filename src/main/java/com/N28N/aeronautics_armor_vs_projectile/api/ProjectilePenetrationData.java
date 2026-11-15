package com.N28N.aeronautics_armor_vs_projectile.api;

import net.minecraft.world.level.block.state.BlockState;

public interface ProjectilePenetrationData {
    int aavp$getPenetrationLevel();

    BlockState aavp$getRenderedBlockState();

    void aavp$setPenetrationData(int penetrationLevel, BlockState renderedBlockState);
}
