package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.N28N.aeronautics_armor_vs_projectile.api.ProjectilePenetrationData;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

@Mixin(value = AbstractBigCannonProjectile.class, remap = false)
public abstract class BigCannonProjectileDataMixin implements ProjectilePenetrationData {
    @Unique
    private static final EntityDataAccessor<Integer> AAVP_PENETRATION_LEVEL =
            SynchedEntityData.defineId(AbstractBigCannonProjectile.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Integer> AAVP_RENDERED_BLOCK_STATE =
            SynchedEntityData.defineId(AbstractBigCannonProjectile.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void aavp$definePenetrationData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(AAVP_PENETRATION_LEVEL, 0);
        builder.define(AAVP_RENDERED_BLOCK_STATE, -1);
    }

    @Override
    public int aavp$getPenetrationLevel() {
        return ((AbstractBigCannonProjectile) (Object) this).getEntityData().get(AAVP_PENETRATION_LEVEL);
    }

    @Override
    public BlockState aavp$getRenderedBlockState() {
        int stateId = ((AbstractBigCannonProjectile) (Object) this).getEntityData().get(AAVP_RENDERED_BLOCK_STATE);
        return stateId < 0 ? null : Block.stateById(stateId);
    }

    @Override
    public void aavp$setPenetrationData(int penetrationLevel, BlockState renderedBlockState) {
        AbstractBigCannonProjectile projectile = (AbstractBigCannonProjectile) (Object) this;
        projectile.getEntityData().set(AAVP_PENETRATION_LEVEL, penetrationLevel);
        projectile.getEntityData().set(AAVP_RENDERED_BLOCK_STATE,
                renderedBlockState == null ? -1 : Block.getId(renderedBlockState));
    }
}
