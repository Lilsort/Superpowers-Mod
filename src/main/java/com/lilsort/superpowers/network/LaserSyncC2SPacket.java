package com.lilsort.superpowers.network;

import com.lilsort.superpowers.laser.LaserPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

/**
 * Client-to-Server packet for syncing laser firing state and eye positions.
 *
 * Payload:
 *   boolean firing
 *   if firing:
 *     double leftEyeX, leftEyeY, leftEyeZ
 *     double rightEyeX, rightEyeY, rightEyeZ
 *     double lookDirX, lookDirY, lookDirZ
 */
public class LaserSyncC2SPacket {

    public static final ResourceLocation ID = new ResourceLocation("superpowers", "laser_sync");

    /**
     * Handles incoming laser sync packets from clients.
     */
    public static void receive(MinecraftServer server, ServerPlayer player,
                                ServerGamePacketListenerImpl handler, FriendlyByteBuf buf,
                                PacketSender responseSender) {
        boolean firing = buf.readBoolean();

        Vec3 leftEye = Vec3.ZERO;
        Vec3 rightEye = Vec3.ZERO;
        Vec3 lookDir = Vec3.ZERO;

        if (firing) {
            leftEye = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            rightEye = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            lookDir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        // Capture final for lambda
        final Vec3 fLeft = leftEye;
        final Vec3 fRight = rightEye;
        final Vec3 fLook = lookDir;

        server.execute(() -> {
            if (player instanceof LaserPlayer laserPlayer) {
                // Only accept firing state if laser is enabled for this player
                if (!laserPlayer.isLaserEnabled()) {
                    laserPlayer.setFiringLaser(false);
                    return;
                }

                laserPlayer.setFiringLaser(firing);
                if (firing) {
                    laserPlayer.setLeftEyePos(fLeft);
                    laserPlayer.setRightEyePos(fRight);
                    laserPlayer.setHeadLookVector(fLook);
                }
            }
        });
    }
}
