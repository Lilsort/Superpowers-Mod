package com.lilsort.superpowers.network;

import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SuperpowerStateSyncS2CPacket {
    public static final ResourceLocation ID = new ResourceLocation("superpowers", "state_sync");

    public static void send(ServerPlayer player) {
        FriendlyByteBuf buf = writeState(player);
        
        // Send to trackers
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracker, ID, buf);
        }
        // Send to self
        ServerPlayNetworking.send(player, ID, buf);
    }

    public static void sendToPlayer(ServerPlayer target, ServerPlayer trackedPlayer) {
        FriendlyByteBuf buf = writeState(trackedPlayer);
        ServerPlayNetworking.send(target, ID, buf);
    }

    private static FriendlyByteBuf writeState(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(player.getId());

        FlightPlayer fp = (FlightPlayer) player;
        buf.writeBoolean(fp.isSuperFlightEnabled());
        buf.writeBoolean(fp.isActuallyFlying());
        buf.writeBoolean(fp.isActuallySprinting());
        buf.writeFloat(fp.getFlightSpeedMultiplier());

        LaserPlayer lp = (LaserPlayer) player;
        buf.writeBoolean(lp.isLaserEnabled());
        buf.writeBoolean(lp.isFiringLaser());
        buf.writeFloat(lp.getLaserGap());
        buf.writeFloat(lp.getLaserOffset());
        buf.writeFloat(lp.getLaserDelay());
        buf.writeFloat(lp.getLaserDistance());
        buf.writeFloat(lp.getLaserDamage());
        buf.writeUtf(lp.getLaserColor());
        buf.writeFloat(lp.getLeftLaserDistance());
        buf.writeFloat(lp.getRightLaserDistance());

        GrabPlayer gp = (GrabPlayer) player;
        buf.writeBoolean(gp.canGrab());
        buf.writeBoolean(gp.isTryingToGrab());
        buf.writeInt(gp.getGrabbedEntity() == null ? -1 : gp.getGrabbedEntity().getId());

        return buf;
    }
}
