package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.DiscordMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.SystemMessage;
import net.zaminmc.zea.chat.model.WritingChannelReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemAndDiscordClassifierTest {
        private final SystemClassifier system = new SystemClassifier();
        private final WritingChannelReportClassifier writingReport = new WritingChannelReportClassifier();
        private final DiscordClassifier discord = new DiscordClassifier();
        private final ClassificationPipeline pipeline = new ClassificationPipeline();

        private CapturedMessage capture(String text) {
                return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
        }

        @Test
        void parsesSystemMessages() {
                var result = system.tryClassify(capture("EarthMC » You are now AFK."));

                SystemMessage payload = assertInstanceOf(SystemMessage.class, result.orElseThrow().payload());
                assertEquals("You are now AFK.", payload.content());
        }

        @Test
        void parsesAfkOff() {
                var result = system.tryClassify(capture("EarthMC » You are no longer AFK."));

                SystemMessage payload = assertInstanceOf(SystemMessage.class, result.orElseThrow().payload());
                assertEquals("You are no longer AFK.", payload.content());
        }

        @Test
        void declinesUnprefixedSystemText() {
                assertTrue(system.tryClassify(capture("You are now AFK.")).isEmpty());
        }

        @Test
        void parsesWritingChannelReport() {
                var result = writingReport.tryClassify(capture(
                                "You are currently in global (write) and 5 other channels"));

                WritingChannelReport payload = assertInstanceOf(WritingChannelReport.class, result.orElseThrow().payload());
                assertEquals("global", payload.writeChannel());
                assertEquals(5, payload.otherChannels());
        }

        @Test
        void parsesDiscordPrefix() {
                var result = discord.tryClassify(capture("[Discord] message forwarded from Discord"));

                DiscordMessage payload = assertInstanceOf(DiscordMessage.class, result.orElseThrow().payload());
                assertEquals("message forwarded from Discord", payload.remainder());
        }

        @Test
        void discordIsNotClassifiedAsGlobalChat() {
                // "[Discord] Author: content" would otherwise parse as an
                // affiliation bracket followed by a display name.
                var classified = pipeline.classify(capture("[Discord] Author: content"));

                assertEquals(MessageCategory.DISCORD, classified.category());
        }

        @Test
        void writingChannelReportBeatsGenericSystemMatch() {
                var result = writingReport.tryClassify(capture(
                                "You are currently in local (write) and 2 other channels"));

                WritingChannelReport payload = assertInstanceOf(WritingChannelReport.class, result.orElseThrow().payload());
                assertEquals("local", payload.writeChannel());
                assertEquals(2, payload.otherChannels());
        }
}
