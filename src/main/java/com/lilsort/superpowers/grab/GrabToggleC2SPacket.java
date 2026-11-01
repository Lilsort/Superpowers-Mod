package com.lilsort.superpowers.grab;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class GrabToggleC2SPacket {

    public static final ResourceLocation ID = new ResourceLocation("superpowers", "grab_toggle");

    public static void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            if (!(player instanceof GrabPlayer grabPlayer)) return;

            if (!grabPlayer.canGrab()) return;

            if (grabPlayer.getGrabbedEntity() != null) {
                grabPlayer.releaseEntity();
            } else if (grabPlayer.isTryingToGrab()) {
                grabPlayer.setTryingToGrab(false);
            } else {
                grabPlayer.setTryingToGrab(true);
            }
        });
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, GrabToggleC2SPacket::receive);
    }
}
