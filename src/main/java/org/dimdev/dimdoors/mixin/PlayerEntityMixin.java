package org.dimdev.dimdoors.mixin;

import org.dimdev.dimdoors.entity.stat.ModStats;
import org.dimdev.dimdoors.world.ModBiomes;
import org.dimdev.dimdoors.world.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

@Mixin(value = PlayerEntity.class, priority = 900)
public abstract class PlayerEntityMixin extends LivingEntity {
	@Shadow
	public abstract void incrementStat(Identifier stat);

	public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
	public void handleLimboFallDamage(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Boolean> cir) {
		if (this.world.getBiome(this.getBlockPos()) == ModBiomes.LIMBO_BIOME) {
			cir.setReturnValue(false);
		}
	}
	/*
	@Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
	public void checkDeath(DamageSource source, CallbackInfo ci) {
		this.doOnDeathStuff(source, ci);
	}
	*/
	@Unique
	protected void doOnDeathStuff(DamageSource source, CallbackInfo ci) {
		if (ModDimensions.isPocketDimension(this.world)) {
			this.removed = false;
			this.dead = false;
			this.setHealth(this.getMaxHealth());
			this.incrementStat(ModStats.DEATHS_IN_POCKETS);
			ci.cancel();
		}
	}
}
