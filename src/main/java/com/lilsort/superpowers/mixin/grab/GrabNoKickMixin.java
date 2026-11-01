package com.lilsort.superpowers.mixin.grab;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class GrabNoKickMixin {

    @Shadow public ServerPlayer player;
    @Shadow private int aboveGroundTickCount;
    @Shadow private int aboveGroundVehicleTickCount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void superpowers$resetAntiCheatWhenGrabbed(CallbackInfo ci) {
        // Find if this player is grabbed by anyone
        boolean isGrabbed = false;
        for (net.minecraft.world.entity.player.Player p : player.level().players()) {
            if (p instanceof com.lilsort.superpowers.grab.GrabPlayer gp && gp.getGrabbedEntity() == player) {
                isGrabbed = true;
                break;
            }
        }

        if (isGrabbed) {
            // Reset the "floating too long" counters to prevent kicking
            this.aboveGroundTickCount = 0;
            this.aboveGroundVehicleTickCount = 0;
        }
    }
}
