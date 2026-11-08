package net.zaminmc.zea.lifecycle;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.earthmc.EarthMcDetector;

/**
 * Client lifecycle integration: initialization wiring, server join/leave
 * handling and shutdown cleanup.
 *
 * <p>All state here is confined to the client thread (event callbacks and
 * render logic all run there).
 */
public final class ClientLifecycle {
	private volatile boolean onEarthMc;

	public ClientLifecycle() {
		register();
	}

	private void register() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onServerJoin(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onServerLeave());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> onClientStopping());
	}

	public boolean onEarthMc() {
		return onEarthMc;
	}

	private void onServerJoin(Minecraft client) {
		ServerData server = client.getCurrentServer();
		onEarthMc = server != null && EarthMcDetector.isEarthMcAddress(server.ip);
		if (onEarthMc) {
			ZeaClient.LOGGER.info("Connected to EarthMC; EarthMC features enabled");
		}
	}

	private void onServerLeave() {
		onEarthMc = false;
	}

	private void onClientStopping() {
		// Shutdown cleanup is registered here as subsystems gain state that
		// must be flushed (persistent stores, executors).
	}
}
