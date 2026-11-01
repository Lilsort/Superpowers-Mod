package com.lilsort.superpowers.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Registers the laser renderer with Fabric's world render events.
 */
public class LaserRenderManager {

    public static void register() {
        WorldRenderEvents.LAST.register(LaserRenderer::renderLasers);
    }
}
