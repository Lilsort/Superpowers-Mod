package com.lilsort.superpowers.mixin.entity;

import com.lilsort.superpowers.grab.GrabPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EntityDamageMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void superpowers$cancelGrabSuffocation(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.getMsgId().equals("inWall")) {
            LivingEntity self = (LivingEntity) (Object) this;

            for (Player player : self.level().players()) {
                if (player instanceof GrabPlayer grabPlayer) {
                    if (grabPlayer.getGrabbedEntity() == self) {
                        // The entity is being grabbed! Cancel suffocation and fall damage.
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }
}
