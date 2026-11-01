package com.lilsort.superpowers.client.input;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.network.LaserSyncC2SPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Handles the Z key for firing the laser.
 * Sends C2S packets to the server with firing state and eye positions.
 */
public class LaserKeybindHandler {

    private static boolean wasFiring = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LaserKeybindHandler::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!(player instanceof LaserPlayer laserPlayer)) return;

        // Only fire if laser ability is enabled
        if (!laserPlayer.isLaserEnabled()) {
            if (wasFiring) {
                stopFiring(laserPlayer);
            }
            return;
        }

        long window = mc.getWindow().getWindow();
        boolean keyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z);

        if (keyDown) {
            laserPlayer.setFiringLaser(true);
            sendC2SPacket(true, laserPlayer);
            wasFiring = true;
        } else if (wasFiring) {
            stopFiring(laserPlayer);
        }
    }

    private static void stopFiring(LaserPlayer laserPlayer) {
        laserPlayer.setFiringLaser(false);
        sendC2SPacket(false, laserPlayer);
        wasFiring = false;
    }

    private static void sendC2SPacket(boolean firing, LaserPlayer laserPlayer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(firing);

        if (firing) {
            // Write left eye position
            buf.writeDouble(laserPlayer.getLeftEyePos().x);
            buf.writeDouble(laserPlayer.getLeftEyePos().y);
            buf.writeDouble(laserPlayer.getLeftEyePos().z);

            // Write right eye position
            buf.writeDouble(laserPlayer.getRightEyePos().x);
            buf.writeDouble(laserPlayer.getRightEyePos().y);
            buf.writeDouble(laserPlayer.getRightEyePos().z);

            // Write look direction
            buf.writeDouble(laserPlayer.getHeadLookVector().x);
            buf.writeDouble(laserPlayer.getHeadLookVector().y);
            buf.writeDouble(laserPlayer.getHeadLookVector().z);
        }

        ClientPlayNetworking.send(LaserSyncC2SPacket.ID, buf);
    }
}
