package com.lilsort.superpowers.client.render.animation;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;

public class GrabDistanceManager {
    private static class PullbackState {
        float pullback;
        long lastTime;
    }

    private static final Map<LivingEntity, PullbackState> STATES = new WeakHashMap<>();

    public static float getSmoothedPullback(LivingEntity entity, float targetPullback) {
        PullbackState state = STATES.computeIfAbsent(entity, k -> {
            PullbackState s = new PullbackState();
            s.pullback = targetPullback;
            s.lastTime = System.currentTimeMillis();
            return s;
        });

        long now = System.currentTimeMillis();
        float delta = (float) (now - state.lastTime) / 1000.0F;
        
        if (delta > 0.0F) {
            if (delta > 0.1F) delta = 0.1F; // Cap delta
            state.lastTime = now;
            
            if (targetPullback > state.pullback) {
                // Instant increase to avoid clipping into walls
                state.pullback = targetPullback;
            } else {
                // Smooth lerp (exponential decay) when extending (pullback decreasing)
                float smoothFactor = 1.0F - (float) Math.exp(-15.0F * delta);
                state.pullback = net.minecraft.util.Mth.lerp(smoothFactor, state.pullback, targetPullback);
            }
        }
        
        return state.pullback;
    }
}
