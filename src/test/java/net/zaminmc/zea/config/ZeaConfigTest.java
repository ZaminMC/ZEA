package net.zaminmc.zea.config;

import net.zaminmc.zea.chat.feeds.Feeds;
import net.zaminmc.zea.chat.feeds.Feed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeaConfigTest {
        @Test
        void defaultsFollowTheSpec() {
                ZeaConfig config = new ZeaConfig();
                assertFalse(config.multiSendEnabled(), "multi-channel sending must default to disabled");
                assertEquals(1000, config.dispatchIntervalMillis());
                assertEquals(ZeaConfig.CURRENT_VERSION, config.configVersion());
                assertTrue(config.enabledExtraFeedIds().isEmpty(), "no extra tabs by default");
                assertTrue(config.smoothChatEnabled(), "smooth chat defaults to on");
                assertEquals(200, config.smoothChatDurationMillis());
        }

        @Test
        void smoothChatDurationIsClampedToTheSliderRange() {
                ZeaConfig config = new ZeaConfig();
                config.chatInterface.smoothChatDurationMillis = 0;
                assertEquals(ZeaConfig.MIN_SMOOTH_CHAT_MILLIS, config.smoothChatDurationMillis());
                config.chatInterface.smoothChatDurationMillis = 999_999;
                assertEquals(ZeaConfig.MAX_SMOOTH_CHAT_MILLIS, config.smoothChatDurationMillis());
        }

        @Test
        void dispatchIntervalIsClampedToTheSliderRange() {
                ZeaConfig config = new ZeaConfig();
                config.chat.dispatchIntervalMillis = 0;
                assertEquals(ZeaConfig.MIN_DISPATCH_INTERVAL_MILLIS, config.dispatchIntervalMillis());
                config.chat.dispatchIntervalMillis = 999_999;
                assertEquals(ZeaConfig.MAX_DISPATCH_INTERVAL_MILLIS, config.dispatchIntervalMillis());
        }

        @Test
        void everyExtraFeedIsEnableable() {
                // A feed added to Feeds without a matching option would become
                // invisible to configuration — pin the mapping to the registry.
                ZeaConfig allOn = new ZeaConfig();
                allOn.tabs.discord = true;
                allOn.tabs.votes = true;
                allOn.tabs.crates = true;
                allOn.tabs.deaths = true;
                allOn.tabs.system = true;
                allOn.tabs.unclassified = true;
                Set<String> enabled = allOn.enabledExtraFeedIds();
                for (Feed extra : Feeds.extraFeeds()) {
                        assertTrue(enabled.contains(extra.id()), "extra feed " + extra.id() + " has no config option");
                }
                assertEquals(Feeds.extraFeeds().size(), enabled.size());
        }

        @Test
        void saveLoadRoundTripPreservesValues(@TempDir Path dir) throws IOException {
                Path file = dir.resolve("zea.json");
                ZeaConfigStore store = new ZeaConfigStore(file);
                ZeaConfig original = store.load();
                original.chat.multiSendEnabled = true;
                original.chat.dispatchIntervalMillis = 2500;
                original.tabs.deaths = true;
                store.save(original);

                ZeaConfig loaded = new ZeaConfigStore(file).load();
                assertTrue(loaded.multiSendEnabled());
                assertEquals(2500, loaded.dispatchIntervalMillis());
                assertTrue(loaded.enabledExtraFeedIds().contains(Feeds.DEATHS.id()));
        }

        @Test
        void saveNowWithoutSaveActionIsASilentNoOp() {
                ZeaConfig config = new ZeaConfig();
                config.saveNow(); // must not throw
        }

        @Test
        void corruptedFileFallsBackToDefaultsAndIsPreserved(@TempDir Path dir) throws IOException {
                Path file = dir.resolve("zea.json");
                Files.writeString(file, "{ this is not json");
                ZeaConfig config = new ZeaConfigStore(file).load();
                assertFalse(config.multiSendEnabled());
                assertEquals(1000, config.dispatchIntervalMillis());
                assertTrue(Files.exists(dir.resolve("zea.json.corrupted")), "corrupt file must be preserved");
        }

        @Test
        void serializedFormCarriesSchemaButNotRuntimeState(@TempDir Path dir) throws IOException {
                Path file = dir.resolve("zea.json");
                ZeaConfig config = new ZeaConfigStore(file).load();
                config.saveNow(); // goes through the store's save action
                String json = Files.readString(file);
                assertTrue(json.contains("\"configVersion\""));
                assertTrue(json.contains("\"chat\""));
                assertTrue(json.contains("\"tabs\""));
                assertFalse(json.contains("saveRunnables"), "MoulConfig runtime state must not persist");
                assertFalse(json.contains("saveAction"), "runtime callback must not persist");
        }

        @Test
        void olderSchemaVersionsMigrateAndRewrite(@TempDir Path dir) throws IOException {
                Path file = dir.resolve("zea.json");
                Files.writeString(file, """
                                {"configVersion": 0, "chat": {"multiSendEnabled": true, "dispatchIntervalMillis": 2000}}
                                """);
                ZeaConfig config = new ZeaConfigStore(file).load();
                assertEquals(ZeaConfig.CURRENT_VERSION, config.configVersion());
                assertTrue(config.multiSendEnabled(), "known fields survive a version migration");
                assertEquals(2000, config.dispatchIntervalMillis());
                String rewritten = Files.readString(file);
                assertTrue(rewritten.contains("\"configVersion\": 1"), "migrated file is rewritten immediately");
        }

        @Test
        void missingFieldsKeepTheirDefaults(@TempDir Path dir) throws IOException {
                Path file = dir.resolve("zea.json");
                Files.writeString(file, "{\"chat\": {\"multiSendEnabled\": true}}");
                ZeaConfig config = new ZeaConfigStore(file).load();
                assertTrue(config.multiSendEnabled());
                assertEquals(1000, config.dispatchIntervalMillis(), "omitted fields fall back to defaults");
                assertTrue(config.enabledExtraFeedIds().isEmpty());
        }
}
