package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.grab.GrabPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityCullingMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void superpowers$forceRenderWhenGrabbed(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (entity.level() != null && entity.level().isClientSide) {
            if (entity instanceof LivingEntity) {
                Player holdingPlayer = getHoldingPlayer(entity);
                if (holdingPlayer != null) {
                    Minecraft client = Minecraft.getInstance();
                    // If the entity is being held by a player...
                    if (!superpowers$isHoldingRecursively(holdingPlayer, client.player) && !frustum.isVisible(holdingPlayer.getBoundingBox())) {
                        // If it's held by another player not chained to us and that player is NOT visible, don't render it
                        cir.setReturnValue(false);
                    } else {
                        // If it's held by US (the local player), a player chained to us, or a visible player, FORCE it to render
                        // This prevents it from disappearing when we fly fast and its real server position is outside the frustum
                        cir.setReturnValue(true);
                    }
                }
            }
            
            // Force render players who are firing lasers so their eye positions stay updated when off-screen
            if (entity instanceof com.lilsort.superpowers.laser.LaserPlayer laserPlayer && laserPlayer.isFiringLaser()) {
                cir.setReturnValue(true);
            }
        }
    }

    private Player getHoldingPlayer(Entity entity) {
        if (entity.level() == null) return null;
        for (Player player : entity.level().players()) {
            if (player instanceof GrabPlayer grabPlayer && grabPlayer.getGrabbedEntity() == entity) {
                return player;
            }
        }
        return null;
    }

    private boolean superpowers$isHoldingRecursively(Player holder, Player localPlayer) {
        if (holder == localPlayer) {
            return true;
        }
        Player grabber = superpowers$getGrabber(holder);
        if (grabber != null) {
            return superpowers$isHoldingRecursively(grabber, localPlayer);
        }
        return false;
    }

    private Player superpowers$getGrabber(Player target) {
        if (target.level() == null) return null;
        for (Player p : target.level().players()) {
            if (p instanceof GrabPlayer gp && gp.getGrabbedEntity() == target) {
                return p;
            }
        }
        return null;
    }
}
