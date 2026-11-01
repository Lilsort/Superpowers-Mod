package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class GrabbedEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void superpowers$onRenderPositionFix(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity.level() != null && entity.level().isClientSide) {
            Player holdingPlayer = getHoldingPlayer(entity);
            if (holdingPlayer != null) {
                if (!(entity instanceof Player)) {
                    float playerLerpedYaw = Mth.lerp(partialTicks, holdingPlayer.yBodyRotO, holdingPlayer.yBodyRot);
                    float targetYaw = playerLerpedYaw + 180.0F;

                    entity.setYRot(targetYaw);
                    entity.yRotO = targetYaw;
                    entity.yBodyRot = targetYaw;
                    entity.yBodyRotO = targetYaw;
                    entity.yHeadRot = targetYaw;
                    entity.yHeadRotO = targetYaw;
                }

                if (entity.getVehicle() != null) {
                    entity.getVehicle().setYRot(0.0F);
                }

                Vec3 handPos = null;
                Minecraft client = Minecraft.getInstance();
                boolean isFirstPerson = holdingPlayer == client.player && client.options.getCameraType().isFirstPerson();

                if (isFirstPerson) {
                    Vec3 localHandPos = ((GrabPlayer) holdingPlayer).getFirstPersonLocalHandPos();
                    if (localHandPos != null) {
                        net.minecraft.client.Camera camera = client.gameRenderer.getMainCamera();
                        Vector3f localRotator = new Vector3f((float) localHandPos.x(), (float) localHandPos.y(), (float) localHandPos.z());
                        localRotator.rotateX((float) Math.toRadians(-camera.getXRot()));
                        localRotator.rotateY((float) Math.toRadians(-(camera.getYRot() + 180.0F)));
                        handPos = camera.getPosition().add(localRotator.x(), localRotator.y(), localRotator.z());
                    }
                } else {
                    handPos = ((GrabPlayer) holdingPlayer).getCalculatedHandPos();
                }

                if (handPos != null) {
                    Vec3 lookDir = holdingPlayer.getViewVector(partialTicks).normalize();
                    float pushForce = isFirstPerson ? 0.0F : entity.getBbWidth() * 0.6F;
                    Vec3 idealHandPos = handPos.add(lookDir.scale(pushForce));

                    Vec3 eyePos = holdingPlayer.getEyePosition(partialTicks);
                    net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
                            eyePos, idealHandPos,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            holdingPlayer
                    );
                    net.minecraft.world.phys.HitResult hit = holdingPlayer.level().clip(context);
                    
                    float targetPullback = 0.0F;
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        float entityRadius = entity.getBbWidth() / 2.0F;
                        Vec3 hitLoc = hit.getLocation();
                        Vec3 hitAdjusted = hitLoc.subtract(lookDir.scale(entityRadius));
                        
                        if (hitAdjusted.distanceToSqr(eyePos) < entityRadius * entityRadius) {
                            hitAdjusted = eyePos.add(lookDir.scale(entityRadius));
                        }
                        
                        Vec3 diff = idealHandPos.subtract(hitAdjusted);
                        targetPullback = (float) diff.dot(lookDir);
                        if (targetPullback < 0.0F) targetPullback = 0.0F;
                    }

                    float smoothedPullback = com.lilsort.superpowers.client.render.animation.GrabDistanceManager.getSmoothedPullback(entity, targetPullback);
                    Vec3 adjustedHandPos = idealHandPos.subtract(lookDir.scale(smoothedPullback));

                    double renderX = Mth.lerp(partialTicks, entity.xo, entity.getX());
                    double renderY = Mth.lerp(partialTicks, entity.yo, entity.getY());
                    double renderZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

                    double offsetX = adjustedHandPos.x() - renderX;
                    double offsetY = adjustedHandPos.y() - renderY;
                    double offsetZ = adjustedHandPos.z() - renderZ;

                    poseStack.translate(offsetX, offsetY, offsetZ);
                }
            }
        }
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void superpowers$onSetupTransforms(T entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (entity.level() != null && entity.level().isClientSide) {
            Player holdingPlayer = getHoldingPlayer(entity);
            if (holdingPlayer != null) {
                float grabHeight = entity.getBbHeight() * 0.85F;
                float playerPitch = Mth.lerp(partialTicks, holdingPlayer.xRotO, holdingPlayer.getXRot());

                poseStack.mulPose(Axis.XP.rotationDegrees(playerPitch));

                Minecraft client = Minecraft.getInstance();
                if (holdingPlayer == client.player && client.options.getCameraType().isFirstPerson()) {
                    poseStack.scale(1.4F, 1.4F, 1.4F);
                }

                poseStack.translate(0.0, -grabHeight, 0.0);
            }
        }
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void superpowers$stopLimbFlailing(M model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof LivingEntity living) || getHoldingPlayer((T) living) == null) {
            model.setupAnim((T) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        if (entity instanceof Player) {
            model.setupAnim((T) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        } else {
            // Stop flailing for mobs by zeroing limb swing
            model.setupAnim((T) entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
        }
    }

    private Player getHoldingPlayer(T entity) {
        for (Player player : entity.level().players()) {
            if (player instanceof GrabPlayer grabPlayer && grabPlayer.getGrabbedEntity() == entity) {
                return player;
            }
        }
        return null;
    }
}
