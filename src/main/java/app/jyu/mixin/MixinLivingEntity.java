package app.jyu.mixin;

import app.jyu.PingSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    
    @Inject(method = "damage", at = @At("RETURN"))
    private void onAfterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        PingSystem.onAfterDamage(self, source, amount, cir);
    }

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    public void beforeIsBlocking(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        PingSystem.beforeIsBlocking(self, cir);
    }
} 