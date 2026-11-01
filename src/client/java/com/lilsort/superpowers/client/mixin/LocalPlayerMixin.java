package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.SuperpowersClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void superpowers$clearMovementInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (SuperpowersClient.iAmBeingGrabbed) {
            boolean isCreative = player.isCreative();
            player.input.leftImpulse = 0.0f;
            player.input.forwardImpulse = 0.0f;
            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;
            player.input.jumping = false;
            if (!isCreative) {
                player.input.shiftKeyDown = false;
            }
        }
    }
}
