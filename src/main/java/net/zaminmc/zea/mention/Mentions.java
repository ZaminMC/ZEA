package net.zaminmc.zea.mention;

import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.notify.NotificationSound;
import net.zaminmc.zea.notify.Notifications;

import java.util.function.Supplier;

/**
 * The mentions leaf service: runs mention detection over the classified
 * stream and fans the result out to its consumers — the chat line
 * highlight, the notification coordinator and (when enabled) the
 * auto-responder. Wiring only; matching lives in {@link MentionDetector}.
 *
 * <p>The local player's name is fetched lazily per message: the session
 * does not exist while the mod initializes.
 */
public final class Mentions {
        private final MentionDetector detector = new MentionDetector();
        private final MentionHighlight highlight = new MentionHighlight();
        private final Supplier<String> localName;
        private final Notifications notifications;
        private final NotificationSound sound;
        private final ZeaConfig config;
        private final MentionResponder responder;

        public Mentions(Supplier<String> localName, Notifications notifications, NotificationSound sound,
                        ZeaConfig config, MentionResponder responder) {
                this.localName = localName;
                this.notifications = notifications;
                this.sound = sound;
                this.config = config;
                this.responder = responder;
        }

        /** The classified-stream entry point for mention handling. */
        public void onClassified(ClassifiedMessage message, long now) {
                var mention = detector.detect(message, localName.get());
                if (mention.isEmpty()) {
                        return;
                }
                if (config.mentionHighlightEnabled()) {
                        highlight.remember(mention.get().source().original());
                }
                if (config.mentionNotificationsEnabled()) {
                        notifications.postMention(mention.get(), now);
                        if (config.mentionSoundEnabled()) {
                                sound.play(net.zaminmc.zea.notify.NotificationKind.MENTION);
                        }
                }
                responder.onMention(mention.get(), now);
        }

        /** The highlight state the chat renderer consults. */
        public MentionHighlight highlight() {
                return highlight;
        }
}
