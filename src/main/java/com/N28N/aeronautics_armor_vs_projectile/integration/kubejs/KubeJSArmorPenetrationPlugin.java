package com.N28N.aeronautics_armor_vs_projectile.integration.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.minecraft.core.registries.Registries;

public final class KubeJSArmorPenetrationPlugin implements KubeJSPlugin {
    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(Registries.BLOCK, builders -> {
            builders.add("armor_clone", ArmorBlockBuilder.class, ArmorBlockBuilder::new);
            builders.add("projectile_clone", ProjectileBlockBuilder.class, ProjectileBlockBuilder::new);
        });
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("AAVP", new ArmorPenetrationKubeJS());
    }
}
