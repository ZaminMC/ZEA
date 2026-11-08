package net.zaminmc.zea;

import net.fabricmc.api.ClientModInitializer;
import net.zaminmc.zea.chat.capture.MessageCapture;
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
	private static ClassificationPipeline classification;

	public static ClientLifecycle lifecycle() {
		return lifecycle;
	}

	public static ClassificationPipeline classification() {
		return classification;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("ZEA loading");
		lifecycle = new ClientLifecycle();
		classification = new ClassificationPipeline();

		// Stage 2 wiring: capture EarthMC messages and classify them.
		// Consumers (feeds, history, notifications) subscribe in later stages.
		new MessageCapture(lifecycle::onEarthMc, captured ->
				LOGGER.debug("Classified [{}] {}", classification.classify(captured).category(), captured.text()));
	}
}
