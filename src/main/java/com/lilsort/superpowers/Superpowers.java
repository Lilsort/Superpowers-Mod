package com.lilsort.superpowers;

import com.lilsort.superpowers.command.SpCommands;
import com.lilsort.superpowers.grab.GrabToggleC2SPacket;
import com.lilsort.superpowers.network.LaserSyncC2SPacket;
import com.lilsort.superpowers.sound.LaserSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Superpowers implements ModInitializer {
	public static final String MOD_ID = "superpowers";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Register C2S packet handler for laser sync
		ServerPlayNetworking.registerGlobalReceiver(LaserSyncC2SPacket.ID, LaserSyncC2SPacket::receive);
		GrabToggleC2SPacket.register();
		com.lilsort.superpowers.grab.GrabBreakC2SPacket.register();

		// Register /sp commands
		SpCommands.register();

		// Register sounds
		LaserSounds.init();
		com.lilsort.superpowers.sound.FlightSounds.init();

		// Register S2C superpower state tracking and synchronization events
		net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
			if (trackedEntity instanceof net.minecraft.server.level.ServerPlayer trackedPlayer) {
				com.lilsort.superpowers.network.SuperpowerStateSyncS2CPacket.sendToPlayer(player, trackedPlayer);
			}
		});

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			com.lilsort.superpowers.network.SuperpowerStateSyncS2CPacket.send(handler.player);
		});

		LOGGER.info("Superpowers mod initialized!");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
