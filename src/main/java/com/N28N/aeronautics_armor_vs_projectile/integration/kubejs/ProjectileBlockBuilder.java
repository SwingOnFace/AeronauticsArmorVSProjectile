package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs;

import java.util.function.Supplier;

import com.N28N.aeronautics_armor_vs_projectile.integration.kubejs.block.ClonedProjectileBlock;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.InertProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;

public final class ProjectileBlockBuilder extends TemplateBlockBuilder {
    private int penetrationLevel;

    public ProjectileBlockBuilder(ResourceLocation id) {
        super(id);
        penetrationLevel = 1;
    }

    public ProjectileBlockBuilder penetrationLevel(int level) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Penetration level must be between 1 and 6");
        }
        penetrationLevel = level;
        return this;
    }

    @Override
    public Block createObject() {
        Block template = getTemplate();
        if (!(template instanceof InertProjectileBlock inertTemplate)) {
            throw new IllegalArgumentException("Projectile templates must be CBC inert projectile blocks: " + templateId);
        }
        Supplier<EntityType<? extends AbstractBigCannonProjectile>> entityType =
                () -> ((ProjectileBlock<?>) inertTemplate).getAssociatedEntityType();
        return new ClonedProjectileBlock(BlockBehaviour.Properties.ofFullCopy(template), entityType, penetrationLevel);
    }
}
