package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.grab.GrabPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class HandPositionTrackerMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    public abstract M getModel();

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
        )
    )
    private void superpowers$extractHandPosition(T livingEntity, float f, float tickDelta, PoseStack poseStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        if (livingEntity instanceof GrabPlayer grabPlayer && (grabPlayer.isTryingToGrab() || grabPlayer.getGrabbedEntity() != null)) {
            if (!(this.getModel() instanceof HumanoidModel<?> model)) {
                return;
            }

            poseStack.pushPose();
            model.leftArm.translateAndRotate(poseStack);
            
            // 0.46875 matches the vertical arm length translation (15/32 blocks) from original decompiled mod
            Vector3f localPos = new Vector3f(0.0f, 0.46875f, 0.0f);
            poseStack.last().pose().transformPosition(localPos);
            poseStack.popPose();

            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            localPos.rotateX((float) Math.toRadians(-camera.getXRot()));
            localPos.rotateY((float) Math.toRadians(-(camera.getYRot() + 180.0F)));
            Vec3 exactWorldPos = camera.getPosition().add(localPos.x(), localPos.y(), localPos.z());
            grabPlayer.setCalculatedHandPos(exactWorldPos);
        }
    }
}
