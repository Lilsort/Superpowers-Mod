package com.lilsort.superpowers.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import com.lilsort.superpowers.laser.LaserPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("TAIL"))
    private void superpowers$copyDataOnRespawn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer newPlayer = (ServerPlayer) (Object) this;
        if (oldPlayer instanceof FlightPlayer oldFp && newPlayer instanceof FlightPlayer newFp) {
            newFp.setSuperFlightEnabled(oldFp.isSuperFlightEnabled());
        }
        if (oldPlayer instanceof GrabPlayer oldGp && newPlayer instanceof GrabPlayer newGp) {
            newGp.setCanGrab(oldGp.canGrab());
        }
        if (oldPlayer instanceof LaserPlayer oldLp && newPlayer instanceof LaserPlayer newLp) {
            newLp.setLaserEnabled(oldLp.isLaserEnabled());
            newLp.setLaserColor(oldLp.getLaserColor());
        }
    }
}
