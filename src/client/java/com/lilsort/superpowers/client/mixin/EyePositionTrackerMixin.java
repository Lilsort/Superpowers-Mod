package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.laser.LaserPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extracts pixel-perfect eye positions from the player model during rendering.
 * These positions are used for both local rendering and sending to the server.
 *
 * Injects after setupAnim() in LivingEntityRenderer.render() to ensure the
 * model's head rotation is fully computed.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class EyePositionTrackerMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    public abstract M getModel();

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void superpowers$extractEyePositions(T entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, CallbackInfo ci) {
        if (!(entity instanceof Player player))
            return;
        if (!(player instanceof LaserPlayer laserPlayer))
            return;
        if (!laserPlayer.isFiringLaser())
            return;

        M model = this.getModel();
        if (!(model instanceof PlayerModel<?> playerModel))
            return;

        poseStack.pushPose();
        playerModel.head.translateAndRotate(poseStack);

        // Store the head transformation matrix
        laserPlayer.setHeadMatrix(new Matrix4f(poseStack.last().pose()));

        // Get camera info for coordinate space conversion
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float pitch = (float) Math.toRadians(-camera.getXRot());
        float yaw = (float) Math.toRadians(-(camera.getYRot() + 180.0f));
        float roll = 0.0f;
        if (Minecraft.getInstance().player instanceof com.lilsort.superpowers.client.flight.FlightClientPlayer fcp) {
            float flightRoll = fcp.getFlightCameraRoll(partialTick);
            if (Math.abs(flightRoll) > 0.1f) {
                roll = (float) Math.toRadians(-flightRoll);
            }
        }

        // Eye offset constants (in model pixels, 16 pixels = 1 block)
        float eyeGap = laserPlayer.getLaserGap(); // horizontal distance from center
        float yOffset = laserPlayer.getLaserOffset(); // vertical offset (eyes below top of head)
        float zOffset = -4.2f; // forward offset (front of head)

        // Transform model-space eye positions to world-space
        Vec3 leftEye = transformModelToWorld(poseStack, pitch, yaw, roll, camera,
                new Vector3f(eyeGap, yOffset, zOffset));
        Vec3 rightEye = transformModelToWorld(poseStack, pitch, yaw, roll, camera,
                new Vector3f(-eyeGap, yOffset, zOffset));

        // Get entity look direction
        Vec3 lookDir = entity.getViewVector(partialTick);

        poseStack.popPose();

        laserPlayer.setLeftEyePos(leftEye);
        laserPlayer.setRightEyePos(rightEye);
        laserPlayer.setHeadLookVector(lookDir);
    }

    /**
     * Transforms a point from model pixel space to absolute world space.
     * The PoseStack at this point includes: camera rotation → entity position →
     * body rotation → head rotation.
     * We undo the camera rotation and add the camera world position to get absolute
     * coordinates.
     */
    private static Vec3 transformModelToWorld(PoseStack poseStack, float pitch, float yaw, float roll,
            Camera camera, Vector3f modelPos) {
        // Convert from model pixels (16px = 1 block) to world units
        Vector3f worldPos = new Vector3f(
                modelPos.x() / 16.0f,
                modelPos.y() / 16.0f,
                modelPos.z() / 16.0f);

        // Apply the accumulated PoseStack matrix (entity position + body rotation +
        // head rotation)
        poseStack.last().pose().transformPosition(worldPos);

        // Undo camera rotation to get camera-relative world-axis-aligned coordinates
        // Must undo in reverse order of application: Roll -> Pitch -> Yaw
        worldPos.rotateZ(roll);
        worldPos.rotateX(pitch);
        worldPos.rotateY(yaw);

        // Add camera world position to get absolute world coordinates
        return camera.getPosition().add(worldPos.x(), worldPos.y(), worldPos.z());
    }
}
