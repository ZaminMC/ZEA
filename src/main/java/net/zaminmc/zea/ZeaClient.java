package net.zaminmc.zea;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.zaminmc.zea.buttons.ButtonActivator;
import net.zaminmc.zea.buttons.InventoryButtons;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.config.ZeaConfigStore;
import net.zaminmc.zea.keybind.ZeaKeys;
import net.zaminmc.zea.lifecycle.ClientLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ZEA entrypoint. Performs wiring only; subsystem behavior lives in the
 * packages described in ARCHITECTURE.md.
 */
public final class ZeaClient implements ClientModInitializer {
	public static final String MOD_ID = "zea";
	public static final Logger LOGGER = LoggerFactory.getLogger("ZEA");

	/** How long shutdown waits for the last history write to land. */
	private static final long HISTORY_SHUTDOWN_TIMEOUT_MILLIS = 5_000;

	private static ClientLifecycle lifecycle;
	private static ChatSubsystem chat;
	private static InventoryButtons inventoryButtons;
	private static ZeaConfig config;
	private static ExecutorService historyExecutor;

	public static ClientLifecycle lifecycle() {
		return lifecycle;
	}

	public static ChatSubsystem chat() {
		return chat;
	}

	public static InventoryButtons inventoryButtons() {
		return inventoryButtons;
	}

	public static ZeaConfig config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("ZEA loading");
		Path configDir = FabricLoader.getInstance().getConfigDir();
		config = new ZeaConfigStore(configDir.resolve("zea.json")).load();
		historyExecutor = newHistoryExecutor();
		lifecycle = new ClientLifecycle();
		chat = new ChatSubsystem(
						lifecycle,
						new ClassificationPipeline(),
						config,
						configDir.resolve("zea/history"),
						historyExecutor);
		inventoryButtons = new InventoryButtons(
						configDir.resolve("zea/buttons.json"),
						new ButtonActivator(zea$commandSender(), ButtonActivator.DEFAULT_COOLDOWN_MILLIS));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> inventoryButtons.reset());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdownHistory());
		ZeaKeys.register();
	}

	/**
	 * The bounded, single-threaded executor history writes run on — one
	 * daemon thread, a small bounded queue (writes coalesce upstream, so
	 * more than a couple of pending tasks never happens), rejection
	 * handled by the recorder rather than by blocking the client thread.
	 */
	private static ExecutorService newHistoryExecutor() {
		return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
						new ArrayBlockingQueue<>(4),
						task -> {
							Thread thread = new Thread(task, "ZEA-History");
							thread.setDaemon(true);
							return thread;
						});
	}

	/**
	 * Flush pending history, then drain the executor: submitted work
	 * finishes before shutdown, and the bounded wait never hangs the
	 * game on a stuck disk.
	 */
	private static void shutdownHistory() {
		try {
			chat.shutdownHistory();
		} finally {
			historyExecutor.shutdown();
			try {
				if (!historyExecutor.awaitTermination(HISTORY_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
					LOGGER.warn("History writes did not finish within {} ms; continuing shutdown",
									HISTORY_SHUTDOWN_TIMEOUT_MILLIS);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/** Sends a button's command through the play connection. */
	private static ButtonActivator.CommandSender zea$commandSender() {
		return command -> {
			ClientPacketListener connection = Minecraft.getInstance().getConnection();
			if (connection == null) {
				throw new IllegalStateException("not connected to a server");
			}
			connection.sendCommand(command);
		};
	}
}
