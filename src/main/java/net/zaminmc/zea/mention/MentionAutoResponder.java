package net.zaminmc.zea.mention;

import net.zaminmc.zea.chat.feeds.Channels;
import net.zaminmc.zea.chat.feeds.OutgoingMessage;
import net.zaminmc.zea.chat.feeds.OutgoingRouter;
import net.zaminmc.zea.chat.feeds.WriteChannel;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.ZeaClient;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The opt-in automatic response to mentions. Disabled by default — this
 * is the one Stage 5 behavior that writes to a public channel on its
 * own, so it ships guarded on every axis:
 *
 * <ul>
 *   <li><b>Off by default</b>, and inert until a non-blank response text
 *       is configured.</li>
 *   <li><b>Cooldown</b> — at least the configured seconds between any two
 *       responses.</li>
 *   <li><b>Session cap</b> — a hard maximum per connection, so no
 *       misconfiguration can flood a channel.</li>
 *   <li><b>Loop guard</b> — a mention whose text already contains the
 *       configured response is treated as someone else's auto-response
 *       (or an echo) and never answered, so two ZEA users cannot
 *       ping-pong.</li>
 *   <li><b>Known channel only</b> — responses go to the channel the
 *       mention arrived on when that channel is a verified writable one;
 *       unattributable mentions are never answered.</li>
 * </ul>
 *
 * <p>Responses dispatch through the same conservative path as manual
 * multi-channel sends (one message at a time, spaced by the dispatch
 * interval, cancelled on disconnect).
 */
public final class MentionAutoResponder implements MentionResponder {
        /** Hard ceiling per connection, independent of configuration. */
        private static final int SESSION_CAP = 20;

        private final ZeaConfig config;
        private final Consumer<OutgoingMessage> sender;
        private final OutgoingRouter router = new OutgoingRouter();
        private long lastResponseAt = Long.MIN_VALUE;
        private int sentThisSession;

        public MentionAutoResponder(ZeaConfig config, Consumer<OutgoingMessage> sender) {
                this.config = config;
                this.sender = sender;
        }

        @Override
        public void onMention(Mention mention, long now) {
                if (!shouldRespond(mention, now)) {
                        return;
                }
                channelFor(mention).ifPresent(channel -> {
                        sender.accept(router.route(channel, config.autoResponseText()));
                        lastResponseAt = now;
                        sentThisSession++;
                        ZeaClient.LOGGER.info("Auto-responded to a mention from {} on {}",
                                        mention.speaker(), channel.id());
                });
        }

        /** Resets the cooldown and session counter; the disconnect path. */
        public void reset() {
                lastResponseAt = Long.MIN_VALUE;
                sentThisSession = 0;
        }

        /** How many responses were sent this session. */
        public int responsesThisSession() {
                return sentThisSession;
        }

        private boolean shouldRespond(Mention mention, long now) {
                if (!config.autoRespondEnabled()) {
                        return false;
                }
                String response = config.autoResponseText();
                if (response.isBlank()) {
                        return false;
                }
                if (sentThisSession >= SESSION_CAP) {
                        return false;
                }
                if (lastResponseAt != Long.MIN_VALUE && now - lastResponseAt < config.autoResponseCooldownMillis()) {
                        return false;
                }
                // Loop guard: an incoming message that already carries our
                // response text is an echo of an auto-response, not a person.
                return !mention.content().contains(response);
        }

        /** The writable channel matching where the mention arrived. */
        private static Optional<WriteChannel> channelFor(Mention mention) {
                return Channels.byId(mention.category().name().toLowerCase(Locale.ROOT));
        }
}
