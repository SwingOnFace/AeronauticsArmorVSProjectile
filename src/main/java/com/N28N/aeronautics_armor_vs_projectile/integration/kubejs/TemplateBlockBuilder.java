package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs;

import java.util.List;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public abstract class TemplateBlockBuilder extends BlockBuilder {
    protected ResourceLocation templateId;
    protected ResourceLocation customTexture;

    TemplateBlockBuilder(ResourceLocation id) {
        super(id);
    }

    public TemplateBlockBuilder template(String id) {
        ResourceLocation parsedId = parseId(id, "template block");
        if (!BuiltInRegistries.BLOCK.containsKey(parsedId)) {
            throw new IllegalArgumentException("Unknown template block: " + parsedId);
        }
        templateId = parsedId;
        updateModel();
        return this;
    }

    public TemplateBlockBuilder name(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Block name must not be blank");
        }
        displayName = Component.literal(name);
        return this;
    }

    @Override
    public TemplateBlockBuilder texture(String texture) {
        customTexture = parseId(texture, "texture");
        updateModel();
        return this;
    }

    public TemplateBlockBuilder inheritTexture() {
        customTexture = null;
        updateModel();
        return this;
    }

    protected final Block getTemplate() {
        if (templateId == null) {
            throw new IllegalStateException("A template block must be selected before creating " + id);
        }
        return BuiltInRegistries.BLOCK.get(templateId);
    }

    private void updateModel() {
        if (templateId == null) {
            return;
        }
        parentModel = templateId.withPath("block/" + templateId.getPath());
        textures.clear();
        if (customTexture != null) {
            for (String key : List.of("all", "texture", "particle", "side", "end", "front", "back", "top", "bottom")) {
                textures.put(key, customTexture.toString());
            }
        }
    }

    private static ResourceLocation parseId(String value, String kind) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid " + kind + " resource ID: " + value);
        }
        return parsed;
    }
}
