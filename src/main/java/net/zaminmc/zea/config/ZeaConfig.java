package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder;
import net.zaminmc.zea.chat.feeds.Feeds;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ZEA's configuration, edited through MoulConfig and persisted with the
 * safe-write protocol. Fields are written by the MoulConfig editor via
 * reflection; accessors — not raw fields — apply range checks so a
 * hand-edited file can never produce an unsafe runtime value.
 *
 * <p>Policy for schema changes: {@code configVersion} gates structured
 * migrations. Loading an older file applies the migration steps for its
 * version; fields the old file omits keep their defaults; unknown fields
 * are dropped on the next save. Version 1 has no migration steps.
 */
public final class ZeaConfig extends Config {
        /** Bump when fields change shape; add the matching migration in the store. */
        public static final int CURRENT_VERSION = 1;

        /** Slider bounds, mirrored here so tests can pin the accessible range. */
        public static final int MIN_DISPATCH_INTERVAL_MILLIS = 500;
        public static final int MAX_DISPATCH_INTERVAL_MILLIS = 5000;

        /** Smooth-chat slider bounds. */
        public static final int MIN_SMOOTH_CHAT_MILLIS = 50;
        public static final int MAX_SMOOTH_CHAT_MILLIS = 500;

        /** Chat options. */
        public static final class Chat {
                @ConfigOption(name = "Multi-channel sending", desc = "Allow sending one message to several "
                                + "channels at once with shift-click. Messages are still sent one at a time, "
                                + "spaced by the dispatch interval below.")
                @ConfigEditorBoolean
                @ConfigOrder(0)
                public boolean multiSendEnabled = false;

                @ConfigOption(name = "Dispatch interval", desc = "Milliseconds between messages when "
                                + "sending to multiple channels. Conservative spacing respects server spam limits.")
                @ConfigEditorSlider(minValue = MIN_DISPATCH_INTERVAL_MILLIS, maxValue = MAX_DISPATCH_INTERVAL_MILLIS,
                                minStep = 100)
                @ConfigOrder(1)
                public int dispatchIntervalMillis = 1000;
        }

        /** Chat appearance and motion. */
        public static final class ChatInterface {
                @ConfigOption(name = "Smooth chat", desc = "Animate chat movement: new messages and "
                                + "scrolling glide instead of jumping. Right-click copying and search are "
                                + "unaffected.")
                @ConfigEditorBoolean
                @ConfigOrder(0)
                public boolean smoothChat = true;

                @ConfigOption(name = "Smooth chat duration", desc = "How long one scroll animation "
                                + "takes, in milliseconds. Lower is snappier.")
                @ConfigEditorSlider(minValue = MIN_SMOOTH_CHAT_MILLIS, maxValue = MAX_SMOOTH_CHAT_MILLIS,
                                minStep = 10)
                @ConfigOrder(1)
                public int smoothChatDurationMillis = 200;
        }

        /** Chat tab visibility. The default tabs are always present. */
        public static final class Tabs {
                @ConfigOption(name = "Discord tab", desc = "Show incoming Discord messages as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(0)
                public boolean discord = false;

                @ConfigOption(name = "Votes tab", desc = "Show vote notifications as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(1)
                public boolean votes = false;

                @ConfigOption(name = "Crate wins tab", desc = "Show crate win announcements as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(2)
                public boolean crates = false;

                @ConfigOption(name = "Deaths tab", desc = "Show death messages as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(3)
                public boolean deaths = false;

                @ConfigOption(name = "System tab", desc = "Show server system messages as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(4)
                public boolean system = false;

                @ConfigOption(name = "General tab", desc = "Show messages ZEA could not classify as a chat tab.")
                @ConfigEditorBoolean
                @ConfigOrder(5)
                public boolean unclassified = false;
        }

        private int configVersion = CURRENT_VERSION;

        @Category(name = "Chat", desc = "Outgoing chat and multi-channel sending.")
        @ConfigOrder(0)
        public Chat chat = new Chat();

        @Category(name = "Tabs", desc = "Which additional chat tabs are shown. The default tabs are always visible.")
        @ConfigOrder(1)
        public Tabs tabs = new Tabs();

        @Category(name = "Chat interface", desc = "How the chat looks and moves.")
        @ConfigOrder(2)
        public ChatInterface chatInterface = new ChatInterface();

        /** Set by the store so the MoulConfig editor's saves go through safe-write. */
        private transient Runnable saveAction;

        /** Interval between multi-channel dispatches, clamped to the slider range. */
        public long dispatchIntervalMillis() {
                return Math.clamp(chat.dispatchIntervalMillis,
                                MIN_DISPATCH_INTERVAL_MILLIS, MAX_DISPATCH_INTERVAL_MILLIS);
        }

        /** Whether multi-target sending is enabled; disabled by default per the Phase 1 spec. */
        public boolean multiSendEnabled() {
                return chat.multiSendEnabled;
        }

        /** Whether smooth chat is enabled; on by default. */
        public boolean smoothChatEnabled() {
                return chatInterface.smoothChat;
        }

        /** The smooth-chat animation length, clamped to the slider range. */
        public int smoothChatDurationMillis() {
                return (int) Math.clamp(chatInterface.smoothChatDurationMillis,
                                MIN_SMOOTH_CHAT_MILLIS, MAX_SMOOTH_CHAT_MILLIS);
        }

        /**
         * The extra feed ids whose tabs the user enabled. Ids come from
         * {@link Feeds} so the mapping cannot drift from the registry.
         */
        public Set<String> enabledExtraFeedIds() {
                Set<String> ids = new LinkedHashSet<>();
                if (tabs.discord) {
                        ids.add(Feeds.DISCORD.id());
                }
                if (tabs.votes) {
                        ids.add(Feeds.VOTES.id());
                }
                if (tabs.crates) {
                        ids.add(Feeds.CRATE_WINS.id());
                }
                if (tabs.deaths) {
                        ids.add(Feeds.DEATHS.id());
                }
                if (tabs.system) {
                        ids.add(Feeds.SYSTEM.id());
                }
                if (tabs.unclassified) {
                        ids.add(Feeds.UNCLASSIFIED.id());
                }
                return ids;
        }

        /** The schema version this instance was loaded from (or wrote). */
        public int configVersion() {
                return configVersion;
        }

        /** Marks the instance as migrated to {@link #CURRENT_VERSION}. */
        public void markMigrated() {
                configVersion = CURRENT_VERSION;
        }

        public void setSaveAction(Runnable saveAction) {
                this.saveAction = saveAction;
        }

        @Override
        public void saveNow() {
                if (saveAction != null) {
                        saveAction.run();
                }
        }
}
