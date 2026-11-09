package net.zaminmc.zea.mention;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.Affiliation;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ChannelChat;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.DiscordMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;
import net.zaminmc.zea.chat.model.PrivateMessage;
import net.zaminmc.zea.chat.model.TaggedChannelChat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mention detection: word boundaries, case-insensitivity, self-echo
 * exclusion, category gating and the conservative attribution for
 * unstructured messages.
 */
class MentionDetectorTest {
        private static final String ME = "Zamin";

        private final MentionDetector detector = new MentionDetector();

        @Test
        void detectsSimpleNameInChannelChat() {
                var mention = detector.detect(globalChat("Coriolanus", "hey Zamin come here"), ME);

                assertEquals("Coriolanus", mention.orElseThrow().speaker());
                assertEquals("hey Zamin come here", mention.orElseThrow().content());
        }

        @Test
        void matchingIsCaseInsensitive() {
                assertTrue(detector.detect(globalChat("Coriolanus", "hey zamin!"), ME).isPresent());
                assertTrue(detector.detect(globalChat("Coriolanus", "hey ZAMIN!"), ME).isPresent());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "hey ZaminFan!",
                        "hey xZamin!",
                        "hey Zamin_!",
                        "hey Zamin2!",
                        "hey Zamin_2000!",
        })
        void longerNamesDoNotMatch(String content) {
                assertTrue(detector.detect(globalChat("Coriolanus", content), ME).isEmpty(), content);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "hey Zamin, look",
                        "(Zamin)",
                        "[Zamin]",
                        "Zamin?",
                        "ping: Zamin.",
                        "a Zamin b",
        })
        void boundariesAllowPunctuation(String content) {
                assertTrue(detector.detect(globalChat("Coriolanus", content), ME).isPresent(), content);
        }

        @Test
        void nameInsideLongerWordWithPunctuationStillExcluded() {
                // The char after the name is a word char -> not a boundary.
                assertTrue(detector.detect(globalChat("Coriolanus", "hey Zamins!"), ME).isEmpty());
        }

        @Test
        void ownEchoDoesNotPing() {
                assertTrue(detector.detect(globalChat("Zamin", "I am Zamin"), ME).isEmpty());
        }

        @Test
        void ownEchoMatchesCaseInsensitively() {
                assertTrue(detector.detect(globalChat("zamin", "I am Zamin"), ME).isEmpty());
        }

        @Test
        void speakerNameInSenderPositionOnlyIsNotAMention() {
                // Someone else named similar, speaking normally.
                assertTrue(detector.detect(globalChat("ZaminFan", "hello everyone"), ME).isEmpty());
        }

        @Test
        void detectsMentionInDiscordRelay() {
                var mention = detector.detect(discord("[Discord] pengpy: Zamin can you see this"), ME);

                assertEquals("pengpy", mention.orElseThrow().speaker());
                assertEquals("Zamin can you see this", mention.orElseThrow().content());
        }

        @Test
        void detectsMentionInTaggedChannelChat() {
                var mention = detector.detect(nationChat("Sosnovka", "Coriolanus", "hey Zamin"), ME);

                assertEquals("Coriolanus", mention.orElseThrow().speaker());
                assertEquals("hey Zamin", mention.orElseThrow().content());
        }

        @Test
        void ownEchoInTaggedChannelChatDoesNotPing() {
                assertTrue(detector.detect(nationChat("Sosnovka", "Zamin", "I am Zamin"), ME).isEmpty());
        }

        @Test
        void unstructuredRequiresSeparator() {
                // No "Name: content" shape -> cannot attribute -> no ping.
                assertTrue(detector.detect(unclassified("Zamin joined the town Athens"), ME).isEmpty());
        }

        @Test
        void unstructuredScansOnlyTheContent() {
                var mention = detector.detect(unclassified("Coriolanus: hey Zamin"), ME);

                assertEquals("Coriolanus", mention.orElseThrow().speaker());
                assertTrue(detector.detect(unclassified("Zamin: I said nothing about anyone"), ME).isEmpty());
        }

        @Test
        void unstructuredWithPrefixBeforeSpeakerStillAttributes() {
                var mention = detector.detect(unclassified("[Local] Coriolanus: hey Zamin"), ME);

                assertEquals("Coriolanus", mention.orElseThrow().speaker());
        }

        @Test
        void unstructuredSeparatorInContentScansAfterFirstSeparator() {
                // First ": " ends the speaker segment.
                var mention = detector.detect(unclassified("Coriolanus: note: Zamin was here"), ME);

                assertTrue(mention.isPresent());
        }

        @Test
        void privateMessagesAreNotMentions() {
                var pm = new ClassifiedMessage(
                                capture("[Coriolanus → me] hey Zamin"),
                                MessageCategory.PRIVATE_MESSAGE,
                                new PrivateMessage("Coriolanus", "me", PrivateMessage.Direction.INCOMING, "hey Zamin"));

                assertTrue(detector.detect(pm, ME).isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"SYSTEM", "VOTE", "CRATE_WIN", "DEATH"})
        void nonChatCategoriesAreIgnored(MessageCategory category) {
                var message = new ClassifiedMessage(capture("Zamin: something"), category, MessagePayload.None.INSTANCE);

                assertTrue(detector.detect(message, ME).isEmpty());
        }

        @Test
        void blankLocalNameDetectsNothing() {
                assertTrue(detector.detect(globalChat("Coriolanus", "hey Zamin"), "").isEmpty());
                assertTrue(detector.detect(globalChat("Coriolanus", "hey Zamin"), null).isEmpty());
        }

        private ClassifiedMessage globalChat(String speaker, String content) {
                ChannelChat chat = new ChannelChat(List.of(), Affiliation.parse("Nation|Town"), speaker, content);
                return new ClassifiedMessage(
                                capture("[Nation|Town] " + speaker + ": " + content),
                                MessageCategory.GLOBAL, chat);
        }

        private ClassifiedMessage nationChat(String town, String speaker, String content) {
                TaggedChannelChat chat = new TaggedChannelChat("Nation Chat", Optional.of(town), speaker, content);
                return new ClassifiedMessage(
                                capture("[Nation Chat|" + town + "] " + speaker + ": " + content),
                                MessageCategory.NATION, chat);
        }

        private ClassifiedMessage discord(String text) {
                String remainder = text.substring("[Discord] ".length());
                return new ClassifiedMessage(capture(text), MessageCategory.DISCORD, new DiscordMessage(remainder));
        }

        private ClassifiedMessage unclassified(String text) {
                return ClassifiedMessage.unclassified(capture(text));
        }

        private static CapturedMessage capture(String text) {
                return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
        }
}
