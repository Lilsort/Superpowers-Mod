package com.lilsort.superpowers.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers laser sound events.
 */
public class LaserSounds {

    public static final SoundEvent LASER_ENTRY = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "laser_entry"), 64.0F);
    public static final SoundEvent LASER_LOOP = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "laser_loop"), 64.0F);
    public static final SoundEvent LASER_QUIT = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "laser_quit"), 64.0F);

    public static void init() {
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "laser_entry"), LASER_ENTRY);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "laser_loop"), LASER_LOOP);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "laser_quit"), LASER_QUIT);
    }
}
