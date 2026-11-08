package net.zaminmc.zea.mention;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.Affiliation;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ChannelChat;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.notify.NotificationKind;
import net.zaminmc.zea.notify.NotificationSound;
import net.zaminmc.zea.notify.Notifications;
import net.zaminmc.zea.notify.NotificationSurface;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mention flow through the mentions service: detection fans out to
 * the highlight set, the notification coordinator and the sound, each
 * gated by its own option.
 */
class MentionsTest {
        private static final long NOW = 1_000_000;
        private static final String ME = "Zamin";

        private final RecordingSurface surface = new RecordingSurface();
        private final RecordingSound sound = new RecordingSound();
        private final ZeaConfig config = new ZeaConfig();
        private final Mentions mentions = new Mentions(
                        () -> ME,
                        new Notifications(surface, config, sound),
                        sound,
                        config,
                        MentionResponder.NONE);

        @Test
        void mentionHighlightsAndNotifies() {
                ClassifiedMessage message = globalChat("Coriolanus", "hey Zamin look");
                mentions.onClassified(message, NOW);

                assertTrue(mentions.highlight().isHighlighted(message.captured().original()));
                assertEquals(1, surface.actionBars.size());
                assertTrue(surface.actionBars.get(0).startsWith("Coriolanus mentioned you: hey Zamin look"));
                assertEquals(List.of(NotificationKind.MENTION), sound.played);
        }

        @Test
        void plainChatWithoutMentionDoesNothing() {
                mentions.onClassified(globalChat("Coriolanus", "nice weather"), NOW);

                assertEquals(0, mentions.highlight().size());
                assertEquals(0, surface.actionBars.size());
                assertEquals(0, sound.played.size());
        }

        @Test
        void disabledHighlightStillNotifies() {
                config.mentions.highlight = false;

                mentions.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);

                assertEquals(0, mentions.highlight().size());
                assertEquals(1, surface.actionBars.size());
        }

        @Test
        void disabledNotificationsStillHighlight() {
                config.notifications.mentionNotifications = false;

                mentions.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);

                assertEquals(1, mentions.highlight().size());
                assertEquals(0, surface.actionBars.size());
                assertEquals(0, sound.played.size());
        }

        @Test
        void disabledSoundStaysSilent() {
                config.notifications.mentionSound = false;

                mentions.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);

                assertEquals(1, surface.actionBars.size());
                assertEquals(0, sound.played.size());
        }

        @Test
        void equalPriorityMentionsQueueInsteadOfStacking() {
                mentions.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);
                mentions.onClassified(globalChat("pengpy", "Zamin again"), NOW + 100);

                assertEquals(1, surface.actionBars.size());
        }

        @Test
        void longMentionContentIsTruncated() {
                mentions.onClassified(globalChat("Coriolanus", "Zamin " + "x".repeat(100)), NOW);

                String text = surface.actionBars.get(0);
                assertTrue(text.length() <= "Coriolanus mentioned you: ".length() + 40,
                                "mention preview is bounded");
        }

        @Test
        void responderReceivesMentions() {
                List<Mention> received = new ArrayList<>();
                Mentions wired = new Mentions(() -> ME,
                                new Notifications(surface, config, sound),
                                sound, config, (mention, now) -> received.add(mention));

                wired.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);

                assertEquals(1, received.size());
                assertEquals("Coriolanus", received.get(0).speaker());
        }

        @Test
        void disconnectClearsHighlights() {
                mentions.onClassified(globalChat("Coriolanus", "hey Zamin"), NOW);

                mentions.highlight().clear();

                assertEquals(0, mentions.highlight().size());
        }

        private ClassifiedMessage globalChat(String speaker, String content) {
                ChannelChat chat = new ChannelChat(List.of(), Affiliation.parse("Nation|Town"), speaker, content);
                return new ClassifiedMessage(
                                CapturedMessage.of(Component.literal("[Nation|Town] " + speaker + ": " + content),
                                                Instant.EPOCH),
                                MessageCategory.GLOBAL, chat);
        }

        private static final class RecordingSurface implements NotificationSurface {
                private final List<String> actionBars = new ArrayList<>();

                @Override
                public void showActionBar(net.minecraft.network.chat.Component text) {
                        actionBars.add(text.getString());
                }

                @Override
                public void showTitle(net.minecraft.network.chat.Component title,
                                net.minecraft.network.chat.Component subtitle, int fadeInTicks, int stayTicks,
                                int fadeOutTicks) {
                }

                @Override
                public void clear() {
                }
        }

        private static final class RecordingSound implements NotificationSound {
                private final List<NotificationKind> played = new ArrayList<>();

                @Override
                public void play(NotificationKind kind) {
                        played.add(kind);
                }
        }
}
