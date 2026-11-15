package com.N28N.aeronautics_armor_vs_projectile.mixin;

import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.N28N.aeronautics_armor_vs_projectile.ArmorPenetrationRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorInspectionToolItem;

@Mixin(value = BlockArmorInspectionToolItem.class, remap = false)
public abstract class BlockArmorInspectionMixin {
    @Inject(method = "addBlockArmorInfo", at = @At("TAIL"), remap = false)
    private static void aavp$appendProtectionInfo(List<Component> tooltip, Level level, BlockPos pos,
            BlockState state, CallbackInfo callbackInfo) {
        int protectionLevel = ArmorPenetrationRegistry.getProtectionLevel(level, pos, state);
        double durability = ArmorPenetrationRegistry.getDurability(level, pos, state);
        tooltip.add(Component.translatable("aeronautics_armor_vs_projectile.inspection.protection_level",
                protectionLevel).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("aeronautics_armor_vs_projectile.inspection.durability",
                String.format(Locale.ROOT, "%.1f", durability)).withStyle(ChatFormatting.GOLD));
    }
}
