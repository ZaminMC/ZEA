package net.zaminmc.zea.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.zaminmc.zea.ZeaClient;
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
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.WritingChannelReport;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.lifecycle.ClientLifecycle;

/**
 * Coordinates the chat subsystem: capture, classification, channel state
 * and conservative outgoing dispatch. Wiring only — behavior lives in the
 * classify/feeds packages; presentation (tabs, rendering) arrives in
 * Stage 4 and subscribes to the classified stream.
 */
public final class ChatSubsystem {
        private final ClassificationPipeline classification;
        private final ClassificationCache classifications;
        private final ZeaConfig config;
        private final OutgoingRouter router = new OutgoingRouter();
        private final ChannelState channelState = new ChannelState();
        private final FeedSelection feedSelection = new FeedSelection(Feeds.ALL);
        private final FeedFilter feedFilter = new FeedFilter(classifications(), feedSelection);
        private final MultiChannelDispatcher dispatcher;

        public ChatSubsystem(ClientLifecycle lifecycle, ClassificationPipeline classification, ZeaConfig config) {
                this.classification = classification;
                this.classifications = new ClassificationCache(classification, 512);
                this.config = config;
                this.dispatcher = new MultiChannelDispatcher(
                                this::sendCommand,
                                new DispatchLogger(),
                                config.dispatchIntervalMillis());

                ClientTickEvents.END_CLIENT_TICK.register(client ->
                                dispatcher.tick(System.currentTimeMillis()));
                ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                        dispatcher.cancel();
                        channelState.clear();
                        // Display state is EarthMC context; leaving resets it so the
                        // vanilla chat is untouched on other servers.
                        feedSelection.select(Feeds.ALL);
                        feedFilter.setSearchQuery(null);
                        feedFilter.apply(client.gui.hud.getChat());
                });

                new MessageCapture(lifecycle::onEarthMc, this::onCaptured);
        }

        /** Classified stream entry point; feeds/history/notifications subscribe here in later stages. */
        private void onCaptured(CapturedMessage captured) {
                ClassifiedMessage classified = classifications.classify(captured);
                if (classified.payload() instanceof WritingChannelReport report) {
                        channelState.applyReport(report);
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

        public FeedFilter feedFilter() {
                return feedFilter;
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
