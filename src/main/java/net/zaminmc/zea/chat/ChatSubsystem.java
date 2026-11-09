package net.zaminmc.zea.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.chat.capture.CaptureRecorder;
import net.zaminmc.zea.chat.capture.MessageCapture;
import net.zaminmc.zea.chat.classify.ClassificationCache;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.chat.feeds.ChannelState;
import net.zaminmc.zea.chat.feeds.FeedFilter;
import net.zaminmc.zea.chat.feeds.FeedSelection;
import net.zaminmc.zea.chat.feeds.Feeds;
import net.zaminmc.zea.chat.feeds.MultiChannelDispatcher;
import net.zaminmc.zea.chat.feeds.MultiSendResult;
import net.zaminmc.zea.chat.feeds.OutgoingMessage;
import net.zaminmc.zea.chat.feeds.OutgoingRouter;
import net.zaminmc.zea.chat.feeds.SendTargets;
import net.zaminmc.zea.chat.history.ChatHistory;
import net.zaminmc.zea.chat.history.HistoryEntry;
import net.zaminmc.zea.chat.history.HistoryRecorder;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.WritingChannelReport;
import net.zaminmc.zea.chat.render.MultiSendToast;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.lifecycle.ClientLifecycle;
import net.zaminmc.zea.mention.MentionAutoResponder;
import net.zaminmc.zea.mention.Mentions;
import net.zaminmc.zea.notify.HudNotificationSurface;
import net.zaminmc.zea.notify.Notifications;
import net.zaminmc.zea.notify.UiNotificationSound;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Coordinates the chat subsystem: capture, classification, channel state,
 * conservative outgoing dispatch and notifications. Wiring only — behavior
 * lives in the classify/feeds/notify packages; presentation (tabs,
 * rendering) subscribes to the classified stream.
 */
public final class ChatSubsystem {
	private final ClassificationPipeline classification;
	private final ClassificationCache classifications;
	private final ZeaConfig config;
	private final OutgoingRouter router = new OutgoingRouter();
	private final ChannelState channelState = new ChannelState();
	private final FeedSelection feedSelection = new FeedSelection(Feeds.ALL);
	private final SendTargets sendTargets = new SendTargets();
	private final FeedFilter feedFilter = new FeedFilter(classifications(), feedSelection);
	private final MultiChannelDispatcher dispatcher;
	private final Notifications notifications;
	private final Mentions mentions;
	private final MentionAutoResponder autoResponder;
	private final ChatHistory history;
	private final HistoryRecorder historyRecorder;
	private final CaptureRecorder capture;

	public ChatSubsystem(ClientLifecycle lifecycle, ClassificationPipeline classification, ZeaConfig config,
			CaptureRecorder capture, Path historyDirectory, Executor historyExecutor) {
		this.classification = classification;
		this.classifications = new ClassificationCache(classification, 512);
		this.config = config;
		this.capture = capture;
		this.dispatcher = new MultiChannelDispatcher(
				this::sendCommand,
				new DispatchFeedback(),
				config.dispatchIntervalMillis());
		this.notifications = new Notifications(new HudNotificationSurface(), config, new UiNotificationSound());
		this.autoResponder = new MentionAutoResponder(config,
				message -> dispatcher.dispatch(List.of(message)));
		this.history = new ChatHistory(config.historyMaxEntries());
		this.historyRecorder = new HistoryRecorder(
				history, config, historyExecutor, System::currentTimeMillis,
				Minecraft.getInstance()::execute);
		this.mentions = new Mentions(
				() -> Minecraft.getInstance().getUser().getName(),
				this.notifications,
				new UiNotificationSound(),
				config,
				autoResponder);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// Registered after ClientLifecycle (constructed first in
			// ZeaClient), so the identity is already resolved here.
			lifecycle.earthMcServer()
					.ifPresent(server -> historyRecorder.start(historyDirectory, server));
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			dispatcher.tick(System.currentTimeMillis());
			notifications.tick(System.currentTimeMillis());
			historyRecorder.tick();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			dispatcher.cancel();
			notifications.clear();
			mentions.highlight().clear();
			autoResponder.reset();
			channelState.clear();
			// Flush the session's history, then end it; the next EarthMC
			// join reloads the per-server file.
			historyRecorder.stop();
			// Display state is EarthMC context; leaving resets it so the
			// vanilla chat is untouched on other servers.
			feedSelection.select(Feeds.ALL);
			sendTargets.clear();
			feedFilter.setSearchQuery(null);
			feedFilter.apply(client.gui.hud.getChat());
		});

		new MessageCapture(lifecycle::onEarthMc, this::onCaptured);
	}

	/** Classified stream entry point; feeds, presentation and notifications subscribe here. */
	private void onCaptured(CapturedMessage captured) {
		ClassifiedMessage classified = classifications.classify(captured);
		if (classified.payload() instanceof WritingChannelReport report) {
			channelState.applyReport(report);
		}
		notifications.onClassified(classified, System.currentTimeMillis());
		mentions.onClassified(classified, System.currentTimeMillis());
		if (!captured.text().isBlank()) {
			history.append(HistoryEntry.of(classified));
			historyRecorder.markDirty();
		}
		ZeaClient.LOGGER.debug("Classified [{}] {}", classified.category(), captured.text());
	}

	/**
	 * Sends one message to the shift-click selected channels. Refused
	 * unless the user enabled multi-channel sending; accepted batches
	 * dispatch one message at a time, spaced by the configured interval.
	 */
	public MultiSendResult sendToTargets(SendTargets targets, String message) {
		if (!config.multiSendEnabled()) {
			return MultiSendResult.DISABLED;
		}
		if (targets == null || targets.isEmpty()) {
			return MultiSendResult.NO_TARGETS;
		}
		dispatcher.dispatch(router.route(targets.targets(), message));
		return MultiSendResult.DISPATCHING;
	}

	public ChannelState channelState() {
		return channelState;
	}

	public ClassificationCache classifications() {
		return classifications;
	}

	public FeedSelection feedSelection() {
		return feedSelection;
	}

	/** The shift-click selected multi-send destinations; cleared on disconnect. */
	public SendTargets sendTargets() {
		return sendTargets;
	}

	public FeedFilter feedFilter() {
		return feedFilter;
	}

	public MultiChannelDispatcher dispatcher() {
		return dispatcher;
	}

	public Notifications notifications() {
		return notifications;
	}

	public Mentions mentions() {
		return mentions;
	}

	/** The session's bounded chat history. */
	public ChatHistory history() {
		return history;
	}

	/** Flushes pending history writes; called before the executor closes. */
	public void shutdownHistory() {
		historyRecorder.flush();
	}

	private void sendCommand(OutgoingMessage message) throws Exception {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			throw new IllegalStateException("not connected to a server");
		}
		connection.sendCommand(message.commandLine());
	}

	/**
	 * Dispatch feedback: every outcome is logged; failures additionally
	 * toast, because a stopped multi-send otherwise looks like the server
	 * ignoring the player. Toasts — not chat lines — because the chat HUD
	 * is feed-filtered and would hide them on a channel tab.
	 */
	private static final class DispatchFeedback implements MultiChannelDispatcher.Listener {
		@Override
		public void onDispatched(OutgoingMessage message, int remaining) {
			ZeaClient.LOGGER.debug("Dispatched to {} ({} remaining)", message.command(), remaining);
		}

		@Override
		public void onFailed(OutgoingMessage message, Throwable cause, int cancelled) {
			ZeaClient.LOGGER.warn("Dispatch to {} failed, cancelled {} remaining: {}",
					message.command(), cancelled, cause.toString());
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui != null) {
				MultiSendToast.showDispatchFailed(minecraft, message.command(), cancelled);
			}
		}

		@Override
		public void onCancelled(int remaining) {
			// Disconnect and dispatch replacement both land here; the
			// disconnect reset already explains itself, a replaced batch is
			// immediately superseded by its acceptance toast.
			ZeaClient.LOGGER.info("Cancelled multi-channel dispatch ({} remaining)", remaining);
		}
	}
}
