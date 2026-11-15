package com.N28N.aeronautics_armor_vs_projectile.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

@Mixin(value = AbstractCannonProjectile.class, remap = false)
public interface AbstractCannonProjectileAccess {
    @Invoker("getForces")
    Vec3 aavp$invokeGetForces(Vec3 position, Vec3 velocity);

    @Invoker("canHitSurface")
    boolean aavp$invokeCanHitSurface();

    @Invoker("getBallisticProperties")
    BallisticPropertiesComponent aavp$invokeGetBallisticProperties();

    @Invoker("onImpact")
    boolean aavp$invokeOnImpact(HitResult hitResult, ImpactResult impactResult, ProjectileContext context);

    @Accessor("impactPos")
    void aavp$setImpactPos(BlockPos impactPos);

    @Accessor("massLost")
    void aavp$setMassLost(float massLost);
}
