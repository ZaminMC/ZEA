package net.zaminmc.zea.notify;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.PrivateMessage;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.mention.Mention;

/**
 * The notifications leaf service: consumes classified messages, turns
 * private messages into coordinator notifications, and resolves them when
 * the player replies. Wiring only — the display policy lives in
 * {@link NotificationCoordinator}.
 *
 * <p>An incoming PM shows persistently (until replied, hard-capped by the
 * coordinator) with a soft cue; replying — any outgoing PM to that sender
 * — resolves it. All input already passed the EarthMC gate in the chat
 * capture path.
 */
public final class Notifications {
        /** Hard lifetime of a PM notice; the reply path usually ends it earlier. */
        private static final long PRIVATE_MESSAGE_EXPIRY_MILLIS = 300_000;

        /** How long a mention notice stays on the surface. */
        private static final long MENTION_DURATION_MILLIS = 4_000;

        /** Longest content preview shown on the action bar or title. */
        private static final int CONTENT_PREVIEW_LIMIT = 60;

        /** Longest mention content preview; mentions stay brief. */
        private static final int MENTION_PREVIEW_LIMIT = 40;

        private final NotificationCoordinator coordinator;
        private final ZeaConfig config;
        private final NotificationSound sound;

        public Notifications(NotificationSurface surface, ZeaConfig config, NotificationSound sound) {
                this.coordinator = new NotificationCoordinator(surface);
                this.config = config;
                this.sound = sound;
        }

        /** The classified-stream entry point for PM handling. */
        public void onClassified(ClassifiedMessage message, long now) {
                if (!(message.payload() instanceof PrivateMessage pm)) {
                        return;
                }
                if (pm.direction() == PrivateMessage.Direction.OUTGOING) {
                        coordinator.resolve(pm.recipient(), now);
                        return;
                }
                if (!config.pmNotificationsEnabled()) {
                        return;
                }
                coordinator.post(Notification.persistent(NotificationKind.PRIVATE_MESSAGE,
                                privateMessageText(pm.sender(), pm.content()),
                                config.pmDestination(), PRIVATE_MESSAGE_EXPIRY_MILLIS, pm.sender()), now);
                if (config.pmSoundEnabled()) {
                        sound.play(NotificationKind.PRIVATE_MESSAGE);
                }
        }

        /** Posts a mention notification (from the mentions service). */
        public void postMention(Mention mention, long now) {
                String preview = mention.content().length() <= MENTION_PREVIEW_LIMIT
                                ? mention.content()
                                : mention.content().substring(0, MENTION_PREVIEW_LIMIT - 3) + "...";
                coordinator.post(Notification.temporary(NotificationKind.MENTION,
                                Component.literal(mention.speaker() + " mentioned you: " + preview)
                                                .withStyle(ChatFormatting.GOLD),
                                config.mentionDestination(), MENTION_DURATION_MILLIS), now);
        }

        /** Advances expiry, refresh and queue promotion. */
        public void tick(long now) {
                coordinator.tick(now);
        }

        /** Clears everything; the disconnect path. */
        public void clear() {
                coordinator.clear();
        }

        public NotificationCoordinator coordinator() {
                return coordinator;
        }

        private static Component privateMessageText(String sender, String content) {
                String preview = content.length() <= CONTENT_PREVIEW_LIMIT
                                ? content
                                : content.substring(0, CONTENT_PREVIEW_LIMIT - 3) + "...";
                return Component.literal("PM from " + sender + ": " + preview)
                                .withStyle(ChatFormatting.YELLOW);
        }
}
