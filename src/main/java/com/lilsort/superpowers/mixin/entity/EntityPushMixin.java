package com.lilsort.superpowers.mixin.entity;

import com.lilsort.superpowers.grab.GrabPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void superpowers$cancelGrabPush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        
        if (superpowers$isGrabbed(self) || superpowers$isGrabbed(entity)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean superpowers$isGrabbed(Entity ent) {
        if (ent.level() == null) return false;
        for (Player player : ent.level().players()) {
            if (player instanceof GrabPlayer gp && gp.getGrabbedEntity() == ent) {
                return true;
            }
        }
        return false;
    }
}
