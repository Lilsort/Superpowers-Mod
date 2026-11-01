package com.lilsort.superpowers.client.grab;

import com.lilsort.superpowers.grab.GrabToggleC2SPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class GrabKeybind {

    private static boolean wasKeyDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) return;

            long window = client.getWindow().getWindow();
            boolean isKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_C);

            if (isKeyDown && !wasKeyDown) {
                // Key just pressed
                ClientPlayNetworking.send(GrabToggleC2SPacket.ID, PacketByteBufs.empty());
            }

            wasKeyDown = isKeyDown;
        });
    }
}
