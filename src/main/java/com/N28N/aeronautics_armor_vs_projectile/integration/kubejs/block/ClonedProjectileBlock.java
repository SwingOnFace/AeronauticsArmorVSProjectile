package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs.block;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.N28N.aeronautics_armor_vs_projectile.api.ProjectilePenetrationData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.InertProjectileBlock;

public final class ClonedProjectileBlock extends InertProjectileBlock {
    private final Supplier<EntityType<? extends AbstractBigCannonProjectile>> entityType;
    private final int penetrationLevel;
    private final MapCodec<ClonedProjectileBlock> blockCodec = MapCodec.unit(this);

    public ClonedProjectileBlock(BlockBehaviour.Properties properties,
            Supplier<EntityType<? extends AbstractBigCannonProjectile>> entityType, int penetrationLevel) {
        super(properties);
        this.entityType = entityType;
        this.penetrationLevel = penetrationLevel;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return blockCodec;
    }

    @Override
    public EntityType<? extends AbstractBigCannonProjectile> getAssociatedEntityType() {
        return entityType.get();
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, List<StructureBlockInfo> blocks) {
        return aavp$attachData(super.getProjectile(level, blocks));
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, ItemStack stack) {
        return aavp$attachData(super.getProjectile(level, stack));
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, BlockPos pos, BlockState state) {
        return aavp$attachData(super.getProjectile(level, pos, state));
    }

    private AbstractBigCannonProjectile aavp$attachData(AbstractBigCannonProjectile projectile) {
        if (projectile instanceof ProjectilePenetrationData data) {
            data.aavp$setPenetrationData(penetrationLevel, defaultBlockState());
        }
        return projectile;
    }
}
