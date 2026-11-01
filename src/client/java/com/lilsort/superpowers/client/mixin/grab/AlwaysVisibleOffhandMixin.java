package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.grab.GrabPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Exact replica of the original mod's AlwaysVisibleOffhandMixin.
 * When the offhand is empty and the player is grabbing, this forces
 * the bare arm to render instead of letting vanilla skip it.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = ItemInHandRenderer.class, priority = 1100)
public abstract class AlwaysVisibleOffhandMixin {

    @Shadow
    protected abstract void renderPlayerArm(PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm);

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0),
            cancellable = true
    )
    private void superpowers$forceRenderEmptyOffhand(
            AbstractClientPlayer player, float tickDelta, float pitch,
            InteractionHand hand, float swingProgress, ItemStack item,
            float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers,
            int light, CallbackInfo ci
    ) {
        // Only intercept the OFF_HAND when item is empty and the player is grabbing
        if (hand == InteractionHand.OFF_HAND && item.isEmpty() && !player.isInvisible()) {
            if (player instanceof GrabPlayer gp) {
                boolean isGrabbing = gp.getGrabbedEntity() != null || gp.isTryingToGrab();
                float weight = com.lilsort.superpowers.client.render.animation.GrabAnimationManager.calculateWeight(player, isGrabbing);
                
                // Continue rendering the arm as long as the fade-out animation is playing
                if (isGrabbing || weight > 0.001F) {
                    HumanoidArm offArm = player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
                    
                    // Vanilla uses equipProgress to slide the arm off-screen.
                    // We simulate this so the empty arm smoothly slides down as weight approaches 0.
                    float modifiedEquipProgress = 1.0F - weight;
                    
                    this.renderPlayerArm(matrices, vertexConsumers, light, modifiedEquipProgress, swingProgress, offArm);
                    matrices.popPose();
                    ci.cancel();
                }
            }
        }
    }
}
