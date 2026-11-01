package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.SuperpowersClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void superpowers$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (SuperpowersClient.iAmBeingGrabbed) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void superpowers$onStartUseItem(CallbackInfo ci) {
        if (SuperpowersClient.iAmBeingGrabbed) {
            ci.cancel();
        }
    }
}
