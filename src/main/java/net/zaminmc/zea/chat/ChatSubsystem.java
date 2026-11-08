package net.zaminmc.zea.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.chat.capture.MessageCapture;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.chat.feeds.ChannelState;
import net.zaminmc.zea.chat.feeds.MultiChannelDispatcher;
import net.zaminmc.zea.chat.feeds.OutgoingMessage;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.WritingChannelReport;
import net.zaminmc.zea.lifecycle.ClientLifecycle;

/**
 * Coordinates the chat subsystem: capture, classification, channel state
 * and conservative outgoing dispatch. Wiring only — behavior lives in the
 * classify/feeds packages; presentation (tabs, rendering) arrives in
 * Stage 4 and subscribes to the classified stream.
 */
public final class ChatSubsystem {
	/** Conservative default spacing between multi-channel dispatches. */
	private static final long DISPATCH_INTERVAL_MILLIS = 1000;

	private final ClassificationPipeline classification;
	private final ChannelState channelState = new ChannelState();
	private final MultiChannelDispatcher dispatcher;

	public ChatSubsystem(ClientLifecycle lifecycle, ClassificationPipeline classification) {
		this.classification = classification;
		this.dispatcher = new MultiChannelDispatcher(
				this::sendCommand,
				new DispatchLogger(),
				DISPATCH_INTERVAL_MILLIS);

		ClientTickEvents.END_CLIENT_TICK.register(client ->
				dispatcher.tick(System.currentTimeMillis()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			dispatcher.cancel();
			channelState.clear();
		});

		new MessageCapture(lifecycle::onEarthMc, this::onCaptured);
	}

	/** Classified stream entry point; feeds/history/notifications subscribe here in later stages. */
	private void onCaptured(CapturedMessage captured) {
		ClassifiedMessage classified = classification.classify(captured);
		if (classified.payload() instanceof WritingChannelReport report) {
			channelState.applyReport(report);
		}
		ZeaClient.LOGGER.debug("Classified [{}] {}", classified.category(), captured.text());
	}

	public ChannelState channelState() {
		return channelState;
	}

	public MultiChannelDispatcher dispatcher() {
		return dispatcher;
	}

	private void sendCommand(OutgoingMessage message) throws Exception {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			throw new IllegalStateException("not connected to a server");
		}
		connection.sendCommand(message.commandLine());
	}

	/** Dispatch feedback; surfaced to the chat interface in Stage 4. */
	private static final class DispatchLogger implements MultiChannelDispatcher.Listener {
		@Override
		public void onDispatched(OutgoingMessage message, int remaining) {
			ZeaClient.LOGGER.debug("Dispatched to {} ({} remaining)", message.command(), remaining);
		}

		@Override
		public void onFailed(OutgoingMessage message, Throwable cause, int cancelled) {
			ZeaClient.LOGGER.warn("Dispatch to {} failed, cancelled {} remaining: {}",
					message.command(), cancelled, cause.toString());
		}

		@Override
		public void onCancelled(int remaining) {
			ZeaClient.LOGGER.info("Cancelled multi-channel dispatch ({} remaining)", remaining);
		}
	}
}
