package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.client.render.animation.GrabAnimationManager;
import com.lilsort.superpowers.grab.GrabPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Exact replica of the original mod's FirstPersonGrabMixin.
 * Transforms the first-person arm into the grabbing pose.
 * Uses the same constants and m = -1.0F mirror logic.
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonGrabMixin {

    @Unique
    private static final float GRAB_X = 1.14F;
    @Unique
    private static final float GRAB_Y = 0.22F;
    @Unique
    private static final float GRAB_Z = -0.49F;
    @Unique
    private static final float GRAB_RX = 10.19F;
    @Unique
    private static final float GRAB_RY = 38.98F;
    @Unique
    private static final float GRAB_RZ = -58.58F;

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void superpowers$onRenderFirstPersonGrab(
            AbstractClientPlayer player, float partialTicks, float pitch,
            InteractionHand hand, float swingProgress, ItemStack item,
            float equipProgress, PoseStack poseStack, MultiBufferSource buffer,
            int combinedLight, CallbackInfo ci
    ) {
        if (player instanceof GrabPlayer grabPlayer) {
            boolean isGrabbing = grabPlayer.getGrabbedEntity() != null || grabPlayer.isTryingToGrab();
            float weight = GrabAnimationManager.calculateWeight(player, isGrabbing);
            if (!(weight < 0.001F) || isGrabbing) {
                HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();

                // Only apply to the LEFT arm (the offhand for right-handed players)
                if (currentArm == HumanoidArm.LEFT) {
                    float m = -1.0F;
                    float x = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_X * m);
                    float y = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_Y);
                    float z = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_Z);
                    float rx = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_RX);
                    float ry = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_RY * m);
                    float rz = net.minecraft.util.Mth.lerp(weight, 0.0F, GRAB_RZ * m);

                    poseStack.translate(x, y, z);
                    poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(rx)));
                    poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(ry)));
                    poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(rz)));

                    // Track the hand position for the grab system
                    Minecraft client = Minecraft.getInstance();
                    if (player == client.player && client.options.getCameraType().isFirstPerson()) {
                        poseStack.pushPose();
                        poseStack.translate(-1.5, 0.0, -1.5);
                        Vector3f localPos = poseStack.last().pose().getTranslation(new Vector3f());
                        poseStack.popPose();

                        grabPlayer.setFirstPersonLocalHandPos(new net.minecraft.world.phys.Vec3(localPos.x(), localPos.y(), localPos.z()));

                        net.minecraft.client.Camera camera = client.gameRenderer.getMainCamera();
                        Vector3f worldCalc = new Vector3f(localPos);
                        worldCalc.rotateX((float) Math.toRadians(-camera.getXRot()));
                        worldCalc.rotateY((float) Math.toRadians(-(camera.getYRot() + 180.0F)));

                        net.minecraft.world.phys.Vec3 exactWorldPos = camera.getPosition().add(worldCalc.x(), worldCalc.y(), worldCalc.z());
                        grabPlayer.setCalculatedHandPos(exactWorldPos);
                    }
                }
            }
        }
    }
}
