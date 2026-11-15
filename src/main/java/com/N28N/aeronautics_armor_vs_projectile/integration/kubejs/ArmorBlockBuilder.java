package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs;

import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ArmorBlockBuilder extends TemplateBlockBuilder {
    private int protectionLevel;

    public ArmorBlockBuilder(ResourceLocation id) {
        super(id);
        protectionLevel = 1;
    }

    public ArmorBlockBuilder protectionLevel(int level) {
        validateLevel(level);
        protectionLevel = level;
        return this;
    }

    @Override
    public Block createObject() {
        Block block = new Block(BlockBehaviour.Properties.ofFullCopy(getTemplate()));
        ArmorPenetrationRegistry.registerBlock(block, protectionLevel);
        return block;
    }

    private static void validateLevel(int level) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Protection level must be between 1 and 6");
        }
    }
}
