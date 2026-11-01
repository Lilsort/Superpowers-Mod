package com.lilsort.superpowers.grab;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class GrabBreakC2SPacket {

    public static final ResourceLocation ID = new ResourceLocation("superpowers", "grab_break");

    public static void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            if (player.isCreative()) {
                // Find who is grabbing this player and release them
                for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                    if (otherPlayer instanceof GrabPlayer gp && gp.getGrabbedEntity() == player) {
                        gp.releaseEntity();
                    }
                }
            }
        });
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, GrabBreakC2SPacket::receive);
    }
}
