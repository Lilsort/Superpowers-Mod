package com.lilsort.superpowers.client.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class GrabAnimationManager {
    private static final WeakHashMap<LivingEntity, GrabAnimState> GRAB_STATES = new WeakHashMap<>();

    public static float calculateWeight(LivingEntity entity, boolean isGrabbing) {
        GrabAnimState state = GRAB_STATES.computeIfAbsent(entity, k -> new GrabAnimState());
        long now = System.currentTimeMillis();
        float delta = (float) (now - state.lastTime) / 1000.0F;

        if (delta > 0.0F) {
            if (delta > 0.1F) {
                delta = 0.1F;
            }

            state.lastTime = now;
            float targetWeight = isGrabbing ? 1.0F : 0.0F;
            float smoothFactor = 1.0F - (float) Math.exp(-12.0F * delta);
            state.weight = Mth.lerp(smoothFactor, state.weight, targetWeight);
        }

        return state.weight;
    }

    public static class GrabAnimState {
        public float weight = 0.0F;
        public long lastTime = System.currentTimeMillis();
    }
}
