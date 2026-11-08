package net.zaminmc.zea;

import net.fabricmc.api.ClientModInitializer;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.lifecycle.ClientLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZEA entrypoint. Performs wiring only; subsystem behavior lives in the
 * packages described in ARCHITECTURE.md.
 */
public final class ZeaClient implements ClientModInitializer {
	public static final String MOD_ID = "zea";
	public static final Logger LOGGER = LoggerFactory.getLogger("ZEA");

	private static ClientLifecycle lifecycle;
	private static ChatSubsystem chat;

	public static ClientLifecycle lifecycle() {
		return lifecycle;
	}

	public static ChatSubsystem chat() {
		return chat;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("ZEA loading");
		lifecycle = new ClientLifecycle();
		chat = new ChatSubsystem(lifecycle, new ClassificationPipeline());
	}
}
