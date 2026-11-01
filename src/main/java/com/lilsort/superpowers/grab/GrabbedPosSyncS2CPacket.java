package com.lilsort.superpowers.grab;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class GrabbedPosSyncS2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("superpowers", "grabbed_pos_sync");

    public static void send(ServerPlayer targetPlayer, Vec3 pos, Vec3 velocity, boolean isGrabbed) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isGrabbed);
        if (isGrabbed) {
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
            buf.writeDouble(velocity.x);
            buf.writeDouble(velocity.y);
            buf.writeDouble(velocity.z);
        }
        ServerPlayNetworking.send(targetPlayer, ID, buf);
    }
}
