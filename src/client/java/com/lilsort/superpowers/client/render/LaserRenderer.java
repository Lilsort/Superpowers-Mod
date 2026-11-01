package com.lilsort.superpowers.client.render;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Renders laser beams from eyes for all players who are firing.
 * Based on the original lasereyes-1.2.0 mod approach:
 * - Per-frame client-side raycasts for ALL players (no server distance dependency)
 * - drawPrismLaser using start+end world positions (not lookDir+distance)
 * - First-person: eye positions from camera + player yaw-based right vector
 * - Third-person/other players: eye positions from EyePositionTrackerMixin
 */
public class LaserRenderer {

    private static final ResourceLocation LASER_TEXTURE =
            new ResourceLocation("superpowers", "textures/misc/laser_beam.png");

    /**
     * Main render entry point, called at WorldRenderEvents.LAST.
     */
    public static void renderLasers(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cameraPos = context.camera().getPosition();

        // Calculate the direction vectors from the camera
        org.joml.Vector3f lookVec3f = context.camera().getLookVector();
        org.joml.Vector3f upVec3f = context.camera().getUpVector();
        Vec3 cameraLookVec = new Vec3(lookVec3f.x, lookVec3f.y, lookVec3f.z);
        Vec3 cameraUpVec = new Vec3(upVec3f.x, upVec3f.y, upVec3f.z);
        Vec3 cameraRightVec = cameraLookVec.cross(cameraUpVec).normalize();
        
        // Apply camera roll if flying so origins stay fixed to the screen corners
        float roll = 0.0f;
        if (mc.player instanceof com.lilsort.superpowers.flight.FlightPlayer fp && fp.isSuperFlightEnabled() && mc.player.getAbilities().flying) {
            if (mc.player instanceof com.lilsort.superpowers.client.flight.FlightClientPlayer fcp) {
                roll = fcp.getFlightCameraRoll(context.tickDelta());
            }
        }
        if (roll != 0.0f) {
            org.joml.Quaternionf q = new org.joml.Quaternionf().fromAxisAngleRad(
                (float)cameraLookVec.x, (float)cameraLookVec.y, (float)cameraLookVec.z, (float)Math.toRadians(roll)
            );
            org.joml.Vector3f rv = new org.joml.Vector3f((float)cameraRightVec.x, (float)cameraRightVec.y, (float)cameraRightVec.z);
            rv.rotate(q);
            cameraRightVec = new Vec3(rv.x(), rv.y(), rv.z());
            
            org.joml.Vector3f uv = new org.joml.Vector3f((float)cameraUpVec.x, (float)cameraUpVec.y, (float)cameraUpVec.z);
            uv.rotate(q);
            cameraUpVec = new Vec3(uv.x(), uv.y(), uv.z());
        }

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!(player instanceof LaserPlayer laserPlayer)) continue;
            if (!laserPlayer.isFiringLaser()) continue;

            Vec3 lookDir = laserPlayer.getHeadLookVector();
            Vec3 leftEye = laserPlayer.getLeftEyePos();
            Vec3 rightEye = laserPlayer.getRightEyePos();
            
            if (player == mc.player) {
                if (mc.options.getCameraType().isFirstPerson()) {
                    lookDir = cameraLookVec;
                    float gap = laserPlayer.getLaserGap() / 16.0F;
                    leftEye = cameraPos.add(cameraRightVec.scale(gap)).add(cameraUpVec.scale(-0.05));
                    rightEye = cameraPos.subtract(cameraRightVec.scale(gap)).add(cameraUpVec.scale(-0.05));
                    laserPlayer.setLeftEyePos(leftEye);
                    laserPlayer.setRightEyePos(rightEye);
                    laserPlayer.setHeadLookVector(lookDir);
                }
            }

            if (leftEye.equals(Vec3.ZERO) || rightEye.equals(Vec3.ZERO)) {
                continue;
            }

            float coreWidth = 0.02f;
            float auraR = 1.0f;
            float auraG = 0.0f;
            float auraB = 0.0f;

            try {
                String hex = laserPlayer.getLaserColor();
                if (hex != null) {
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    int colorCode = Integer.parseInt(hex, 16);
                    auraR = (float) ((colorCode >> 16) & 0xFF) / 255.0F;
                    auraG = (float) ((colorCode >> 8) & 0xFF) / 255.0F;
                    auraB = (float) (colorCode & 0xFF) / 255.0F;
                }
            } catch (Exception ignored) {
            }

            float delayTicks = laserPlayer.getLaserDelay() * 20.0f;
            int chargeTicks = laserPlayer.getChargeTicks();

            if (chargeTicks >= delayTicks) {
                // ===== FULLY CHARGED: render beams =====
                double maxRange = laserPlayer.getLaserDistance();

                // Client-side raycasts every frame for the local player to ensure immediate responsiveness.
                // For other players, use the server-synced hit distance to prevent visual desync.
                Vec3 endPointLeft;
                Vec3 endPointRight;

                if (player == mc.player) {
                    HitResult hitLeft = performClientRaycast(player, leftEye, lookDir, maxRange, coreWidth);
                    endPointLeft = hitLeft.getType() != HitResult.Type.MISS
                            ? hitLeft.getLocation()
                            : leftEye.add(lookDir.scale(maxRange));

                    HitResult hitRight = performClientRaycast(player, rightEye, lookDir, maxRange, coreWidth);
                    endPointRight = hitRight.getType() != HitResult.Type.MISS
                            ? hitRight.getLocation()
                            : rightEye.add(lookDir.scale(maxRange));
                } else {
                    float tickDelta = context.tickDelta();
                    float leftDist = laserPlayer.getLerpedLeftLaserDistance(tickDelta);
                    float rightDist = laserPlayer.getLerpedRightLaserDistance(tickDelta);
                    
                    HitResult hitLeft = performClientRaycast(player, leftEye, lookDir, maxRange, coreWidth);
                    endPointLeft = hitLeft.getType() != HitResult.Type.MISS ? hitLeft.getLocation() : leftEye.add(lookDir.scale(maxRange));
                    
                    HitResult hitRight = performClientRaycast(player, rightEye, lookDir, maxRange, coreWidth);
                    endPointRight = hitRight.getType() != HitResult.Type.MISS ? hitRight.getLocation() : rightEye.add(lookDir.scale(maxRange));
                    
                    if (leftEye.distanceTo(endPointLeft) > leftDist) {
                        endPointLeft = leftEye.add(lookDir.scale(leftDist));
                    }
                    if (rightEye.distanceTo(endPointRight) > rightDist) {
                        endPointRight = rightEye.add(lookDir.scale(rightDist));
                    }
                }

                long time = net.minecraft.Util.getMillis();
                float fastPulse = (float) Math.sin((double) time / 25.0);
                float slowPulse = (float) Math.sin((double) time / 120.0);

                float outerAuraWidth = coreWidth * 3.5F + slowPulse * coreWidth * 0.3F;
                float outerAuraAlpha = 0.15F + fastPulse * 0.05F;
                float innerAuraWidth = coreWidth * 1.8F + fastPulse * coreWidth * 0.1F;
                float innerAuraAlpha = 0.4F + slowPulse * 0.1F;
                float animatedCoreWidth = coreWidth + fastPulse * coreWidth * 0.05F;

                RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
                RenderSystem.setShaderTexture(0, LASER_TEXTURE);
                RenderSystem.disableCull();

                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();
                PoseStack matrices = context.matrixStack();
                matrices.pushPose();
                matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                // 1. Opaque Core
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                drawPrismLaser(matrices, buffer, rightEye, endPointRight, lookDir, 1.0F, 1.0F, 1.0F, 1.0F, animatedCoreWidth);
                drawPrismLaser(matrices, buffer, leftEye, endPointLeft, lookDir, 1.0F, 1.0F, 1.0F, 1.0F, animatedCoreWidth);

                // 2. Transparent Auras
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                drawPrismLaser(matrices, buffer, rightEye, endPointRight, lookDir, auraR, auraG, auraB, outerAuraAlpha, outerAuraWidth);
                drawPrismLaser(matrices, buffer, leftEye, endPointLeft, lookDir, auraR, auraG, auraB, outerAuraAlpha, outerAuraWidth);
                drawPrismLaser(matrices, buffer, rightEye, endPointRight, lookDir, auraR, auraG, auraB, innerAuraAlpha, innerAuraWidth);
                drawPrismLaser(matrices, buffer, leftEye, endPointLeft, lookDir, auraR, auraG, auraB, innerAuraAlpha, innerAuraWidth);

                matrices.popPose();
                RenderSystem.enableCull();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();

            } else if (delayTicks > 0.0f) {
                // ===== CHARGING: render glow quads on eyes =====
                float progress = (float) chargeTicks / delayTicks;
                float innerGlowSize = Math.max(coreWidth * 1.7F, 0.02F);
                float outerGlowSize = innerGlowSize * 1.3F;
                float secondOuterGlowSize = innerGlowSize * 1.5F;
                float innerAlpha = progress * 0.85F;
                float outerAlpha = progress * 0.25F;
                float secondOuterAlpha = progress * 0.1F;

                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                PoseStack matrices = context.matrixStack();
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder buffer = tesselator.getBuilder();

                if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
                    float yaw = (float) Math.atan2(lookDir.x, lookDir.z);
                    float pitch = (float) -Math.asin(lookDir.y);
                    matrices.pushPose();
                    matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    drawSimpleGlowQuad(matrices, buffer, leftEye, yaw, pitch, auraR, auraG, auraB, secondOuterAlpha, secondOuterGlowSize);
                    drawSimpleGlowQuad(matrices, buffer, rightEye, yaw, pitch, auraR, auraG, auraB, secondOuterAlpha, secondOuterGlowSize);
                    drawSimpleGlowQuad(matrices, buffer, leftEye, yaw, pitch, auraR, auraG, auraB, outerAlpha, outerGlowSize);
                    drawSimpleGlowQuad(matrices, buffer, rightEye, yaw, pitch, auraR, auraG, auraB, outerAlpha, outerGlowSize);
                    drawSimpleGlowQuad(matrices, buffer, leftEye, yaw, pitch, auraR, auraG, auraB, innerAlpha, innerGlowSize);
                    drawSimpleGlowQuad(matrices, buffer, rightEye, yaw, pitch, auraR, auraG, auraB, innerAlpha, innerGlowSize);
                    tesselator.end();
                    matrices.popPose();
                } else {
                    Matrix4f headMat = laserPlayer.getHeadMatrix();
                    float localGap = laserPlayer.getLaserGap() / 16.0F;
                    float localY = laserPlayer.getLaserOffset() / 16.0F;
                    float localZ = -0.27249998F;

                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    drawMatrixGlowQuad(buffer, headMat, localGap, localY, localZ, auraR, auraG, auraB, secondOuterAlpha, secondOuterGlowSize);
                    drawMatrixGlowQuad(buffer, headMat, localGap, localY, localZ, auraR, auraG, auraB, outerAlpha, outerGlowSize);
                    drawMatrixGlowQuad(buffer, headMat, localGap, localY, localZ, auraR, auraG, auraB, innerAlpha, innerGlowSize);
                    drawMatrixGlowQuad(buffer, headMat, -localGap, localY, localZ, auraR, auraG, auraB, secondOuterAlpha, secondOuterGlowSize);
                    drawMatrixGlowQuad(buffer, headMat, -localGap, localY, localZ, auraR, auraG, auraB, outerAlpha, outerGlowSize);
                    drawMatrixGlowQuad(buffer, headMat, -localGap, localY, localZ, auraR, auraG, auraB, innerAlpha, innerGlowSize);
                    tesselator.end();
                }

                RenderSystem.enableCull();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }
        }
    }

    /**
     * Client-side raycast matching the original lasereyes mod approach.
     * Checks blocks and entity bounding boxes.
     */
    private static HitResult performClientRaycast(AbstractClientPlayer shooter, Vec3 startPos, Vec3 lookDir, double maxRange, float laserWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return BlockHitResult.miss(startPos, net.minecraft.core.Direction.UP, net.minecraft.core.BlockPos.containing(startPos));

        Vec3 endPos = startPos.add(lookDir.scale(maxRange));

        // Block raycast
        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                startPos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                shooter
        ));

        Vec3 maxEntityEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPos;
        AABB searchBox = new AABB(startPos, maxEntityEnd).inflate(laserWidth + 1.0);

        EntityHitResult closestEntityHit = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.level.getEntities(shooter, searchBox, e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable())) {
            float margin = entity.getPickRadius() + laserWidth;
            AABB targetBox = entity.getBoundingBox().inflate(margin);
            Optional<Vec3> hitPos = targetBox.clip(startPos, maxEntityEnd);
            if (hitPos.isPresent()) {
                double distance = startPos.distanceToSqr(hitPos.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntityHit = new EntityHitResult(entity, hitPos.get());
                }
            }
        }

        return closestEntityHit != null ? closestEntityHit : blockHit;
    }

    /**
     * Draws a laser beam as a rectangular tube from start to end.
     * Uses start+end world positions (like original mod) instead of lookDir+distance.
     */
    private static void drawPrismLaser(PoseStack matrices, BufferBuilder buffer, Vec3 start, Vec3 end, Vec3 lookDir, float r, float g, float b, float alpha, float thickness) {
        matrices.pushPose();
        matrices.translate(start.x, start.y, start.z);

        float yaw = (float) Math.atan2(lookDir.x, lookDir.z);
        float pitch = (float) -Math.asin(lookDir.y);
        matrices.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
        matrices.mulPose(com.mojang.math.Axis.XP.rotation(pitch));

        // In first person, roll the cross section so it stays aligned with the screen
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.player instanceof com.lilsort.superpowers.flight.FlightPlayer fp && fp.isSuperFlightEnabled() && mc.player.getAbilities().flying) {
            if (mc.player instanceof com.lilsort.superpowers.client.flight.FlightClientPlayer fcp) {
                float flightRoll = fcp.getFlightCameraRoll(mc.getFrameTime());
                if (Math.abs(flightRoll) > 0.1f) {
                    matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(flightRoll));
                }
            }
        }

        float headDepthOffset = -0.05F;
        matrices.translate(0.0F, 0.0F, headDepthOffset);
        double length = start.distanceTo(end) - headDepthOffset;

        Matrix4f posMatrix = matrices.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        drawRectangularTube(buffer, posMatrix, length, thickness, r, g, b, alpha);
        Tesselator.getInstance().end();

        matrices.popPose();
    }

    /**
     * Draws a rectangular tube (6-face box) from z=0 to z=length in local space.
     */
    private static void drawRectangularTube(BufferBuilder buffer, Matrix4f matrix,
                                             double length, float hw,
                                             float r, float g, float b, float a) {
        float l = (float) length;
        // Top face
        vertex(buffer, matrix, -hw, hw, 0, r, g, b, a, 0, 0);
        vertex(buffer, matrix, -hw, hw, l, r, g, b, a, 0, 1);
        vertex(buffer, matrix, hw, hw, l, r, g, b, a, 1, 1);
        vertex(buffer, matrix, hw, hw, 0, r, g, b, a, 1, 0);
        // Bottom face
        vertex(buffer, matrix, hw, -hw, 0, r, g, b, a, 1, 0);
        vertex(buffer, matrix, hw, -hw, l, r, g, b, a, 1, 1);
        vertex(buffer, matrix, -hw, -hw, l, r, g, b, a, 0, 1);
        vertex(buffer, matrix, -hw, -hw, 0, r, g, b, a, 0, 0);
        // Left face
        vertex(buffer, matrix, -hw, -hw, 0, r, g, b, a, 0, 0);
        vertex(buffer, matrix, -hw, -hw, l, r, g, b, a, 0, 1);
        vertex(buffer, matrix, -hw, hw, l, r, g, b, a, 1, 1);
        vertex(buffer, matrix, -hw, hw, 0, r, g, b, a, 1, 0);
        // Right face
        vertex(buffer, matrix, hw, hw, 0, r, g, b, a, 1, 0);
        vertex(buffer, matrix, hw, hw, l, r, g, b, a, 1, 1);
        vertex(buffer, matrix, hw, -hw, l, r, g, b, a, 0, 1);
        vertex(buffer, matrix, hw, -hw, 0, r, g, b, a, 0, 0);
        // Back face (end cap)
        vertex(buffer, matrix, -hw, hw, l, r, g, b, a, 0, 0);
        vertex(buffer, matrix, -hw, -hw, l, r, g, b, a, 0, 1);
        vertex(buffer, matrix, hw, -hw, l, r, g, b, a, 1, 1);
        vertex(buffer, matrix, hw, hw, l, r, g, b, a, 1, 0);
        // Front face (start cap)
        vertex(buffer, matrix, hw, hw, 0, r, g, b, a, 0, 0);
        vertex(buffer, matrix, hw, -hw, 0, r, g, b, a, 0, 1);
        vertex(buffer, matrix, -hw, -hw, 0, r, g, b, a, 1, 1);
        vertex(buffer, matrix, -hw, hw, 0, r, g, b, a, 1, 0);
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix,
                                float x, float y, float z,
                                float r, float g, float b, float a,
                                float u, float v) {
        Vector3f pos = new Vector3f(x, y, z);
        matrix.transformPosition(pos);
        builder.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .uv(u, v)
                .endVertex();
    }

    private static void drawSimpleGlowQuad(PoseStack matrices, BufferBuilder buffer, Vec3 start, float yaw, float pitch, float r, float g, float b, float alpha, float size) {
        matrices.pushPose();
        matrices.translate(start.x, start.y, start.z);
        matrices.mulPose(new Quaternionf().rotateY(yaw));
        matrices.mulPose(new Quaternionf().rotateX(pitch));
        matrices.translate(0.0F, 0.0F, 0.001F);
        Matrix4f posMatrix = matrices.last().pose();

        vertexColor(buffer, posMatrix, -size, size, 0.0F, r, g, b, alpha);
        vertexColor(buffer, posMatrix, -size, -size, 0.0F, r, g, b, alpha);
        vertexColor(buffer, posMatrix, size, -size, 0.0F, r, g, b, alpha);
        vertexColor(buffer, posMatrix, size, size, 0.0F, r, g, b, alpha);
        matrices.popPose();
    }

    private static void drawMatrixGlowQuad(BufferBuilder buffer, Matrix4f headMat, float cx, float cy, float cz, float r, float g, float b, float alpha, float size) {
        vertexColor(buffer, headMat, cx - size, cy + size, cz, r, g, b, alpha);
        vertexColor(buffer, headMat, cx - size, cy - size, cz, r, g, b, alpha);
        vertexColor(buffer, headMat, cx + size, cy - size, cz, r, g, b, alpha);
        vertexColor(buffer, headMat, cx + size, cy + size, cz, r, g, b, alpha);
    }

    private static void vertexColor(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        Vector3f pos = new Vector3f(x, y, z);
        matrix.transformPosition(pos);
        builder.vertex(pos.x(), pos.y(), pos.z())
               .color(r, g, b, a)
               .endVertex();
    }
}
