package app.jyu.mixin;

import app.jyu.QuizCraft;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Inject(
      method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
      at = @At("HEAD")
    )
    public void onDamageStart(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        QuizCraft.onDamageStart(self, source, amount, cir);
    }
    
    @Redirect(
      method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
      at = @At(
        value  = "INVOKE",
        target = "Lnet/minecraft/entity/LivingEntity;blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)Z"
      )
    )
    public boolean redirectBlockedByShield(LivingEntity self, DamageSource source) {
        return QuizCraft.redirectBlockedByShield(self, source);
    }
} 