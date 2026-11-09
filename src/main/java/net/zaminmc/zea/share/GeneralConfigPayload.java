package net.zaminmc.zea.share;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.notify.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * The general-configuration share payload: the behavior settings a
 * player would want to hand a friend — chat, tabs, chat interface,
 * notifications, mentions and the inventory-button toggle.
 *
 * <p>Excluded by design, not by filtering after the fact: chat history
 * (local storage policy, machine-specific in spirit) and everything
 * internal (schema versions, save hooks). Nothing in ZEA's
 * configuration is a credential, but the allowlist is still the rule —
 * a future field with no business being shared will not leak by
 * accident.
 *
 * <p>Every field is optional in an import (absent means "keep mine"),
 * and validation completes for the whole document before any field is
 * applied — one bad value rejects the import instead of half-applying.
 */
public final class GeneralConfigPayload implements SharePayload {
        /** Longest imported auto-response text; longer text is truncated. */
        public static final int MAX_AUTO_RESPONSE_LENGTH = 256;

        private final Boolean multiSendEnabled;
        private final Integer dispatchIntervalMillis;
        private final Boolean smoothChat;
        private final Integer smoothChatDurationMillis;
        private final Boolean tabDiscord;
        private final Boolean tabVotes;
        private final Boolean tabCrates;
        private final Boolean tabDeaths;
        private final Boolean tabSystem;
        private final Boolean tabUnclassified;
        private final Boolean pmNotifications;
        private final Boolean pmSound;
        private final Notification.Destination pmDestination;
        private final Boolean mentionNotifications;
        private final Boolean mentionSound;
        private final Notification.Destination mentionDestination;
        private final Boolean mentionHighlight;
        private final Boolean autoRespond;
        private final String autoResponseText;
        private final Integer autoResponseCooldownSeconds;
        private final Boolean inventoryButtonsEnabled;

        private GeneralConfigPayload(
                        Boolean multiSendEnabled, Integer dispatchIntervalMillis,
                        Boolean smoothChat, Integer smoothChatDurationMillis,
                        Boolean tabDiscord, Boolean tabVotes, Boolean tabCrates, Boolean tabDeaths,
                        Boolean tabSystem, Boolean tabUnclassified,
                        Boolean pmNotifications, Boolean pmSound, Notification.Destination pmDestination,
                        Boolean mentionNotifications, Boolean mentionSound,
                        Notification.Destination mentionDestination,
                        Boolean mentionHighlight, Boolean autoRespond, String autoResponseText,
                        Integer autoResponseCooldownSeconds, Boolean inventoryButtonsEnabled) {
                this.multiSendEnabled = multiSendEnabled;
                this.dispatchIntervalMillis = dispatchIntervalMillis;
                this.smoothChat = smoothChat;
                this.smoothChatDurationMillis = smoothChatDurationMillis;
                this.tabDiscord = tabDiscord;
                this.tabVotes = tabVotes;
                this.tabCrates = tabCrates;
                this.tabDeaths = tabDeaths;
                this.tabSystem = tabSystem;
                this.tabUnclassified = tabUnclassified;
                this.pmNotifications = pmNotifications;
                this.pmSound = pmSound;
                this.pmDestination = pmDestination;
                this.mentionNotifications = mentionNotifications;
                this.mentionSound = mentionSound;
                this.mentionDestination = mentionDestination;
                this.mentionHighlight = mentionHighlight;
                this.autoRespond = autoRespond;
                this.autoResponseText = autoResponseText;
                this.autoResponseCooldownSeconds = autoResponseCooldownSeconds;
                this.inventoryButtonsEnabled = inventoryButtonsEnabled;
        }

        /** Builds an export payload from the live configuration. */
        public static GeneralConfigPayload of(ZeaConfig config) {
                return new GeneralConfigPayload(
                                config.chat.multiSendEnabled,
                                config.chat.dispatchIntervalMillis,
                                config.chatInterface.smoothChat,
                                config.chatInterface.smoothChatDurationMillis,
                                config.tabs.discord,
                                config.tabs.votes,
                                config.tabs.crates,
                                config.tabs.deaths,
                                config.tabs.system,
                                config.tabs.unclassified,
                                config.notifications.pmNotifications,
                                config.notifications.pmSound,
                                config.notifications.pmDestination,
                                config.notifications.mentionNotifications,
                                config.notifications.mentionSound,
                                config.notifications.mentionDestination,
                                config.mentions.highlight,
                                config.mentions.autoRespond,
                                config.mentions.autoResponseText,
                                config.mentions.autoResponseCooldownSeconds,
                                config.inventoryButtons.enabled);
        }

        /**
         * Decodes and validates an imported payload.
         *
         * @throws ShareException naming the first field that is present
         *                         but not a valid value
         */
        public static GeneralConfigPayload fromJson(JsonObject data) throws ShareException {
                return new GeneralConfigPayload(
                                bool(data, "chat", "multiSendEnabled"),
                                wholeNumber(data, "chat", "dispatchIntervalMillis"),
                                bool(data, "chatInterface", "smoothChat"),
                                wholeNumber(data, "chatInterface", "smoothChatDurationMillis"),
                                bool(data, "tabs", "discord"),
                                bool(data, "tabs", "votes"),
                                bool(data, "tabs", "crates"),
                                bool(data, "tabs", "deaths"),
                                bool(data, "tabs", "system"),
                                bool(data, "tabs", "unclassified"),
                                bool(data, "notifications", "pmNotifications"),
                                bool(data, "notifications", "pmSound"),
                                destination(data, "notifications", "pmDestination"),
                                bool(data, "notifications", "mentionNotifications"),
                                bool(data, "notifications", "mentionSound"),
                                destination(data, "notifications", "mentionDestination"),
                                bool(data, "mentions", "highlight"),
                                bool(data, "mentions", "autoRespond"),
                                text(data, "mentions", "autoResponseText"),
                                wholeNumber(data, "mentions", "autoResponseCooldownSeconds"),
                                bool(data, "inventoryButtons", "enabled"));
        }

        /** Serializes the payload's {@code data} object. */
        public JsonObject toJson() {
                JsonObject data = new JsonObject();
                JsonObject chat = new JsonObject();
                chat.addProperty("multiSendEnabled", multiSendEnabled);
                chat.addProperty("dispatchIntervalMillis", dispatchIntervalMillis);
                JsonObject chatInterface = new JsonObject();
                chatInterface.addProperty("smoothChat", smoothChat);
                chatInterface.addProperty("smoothChatDurationMillis", smoothChatDurationMillis);
                JsonObject tabs = new JsonObject();
                tabs.addProperty("discord", tabDiscord);
                tabs.addProperty("votes", tabVotes);
                tabs.addProperty("crates", tabCrates);
                tabs.addProperty("deaths", tabDeaths);
                tabs.addProperty("system", tabSystem);
                tabs.addProperty("unclassified", tabUnclassified);
                JsonObject notifications = new JsonObject();
                notifications.addProperty("pmNotifications", pmNotifications);
                notifications.addProperty("pmSound", pmSound);
                addDestination(notifications, "pmDestination", pmDestination);
                notifications.addProperty("mentionNotifications", mentionNotifications);
                notifications.addProperty("mentionSound", mentionSound);
                addDestination(notifications, "mentionDestination", mentionDestination);
                JsonObject mentions = new JsonObject();
                mentions.addProperty("highlight", mentionHighlight);
                mentions.addProperty("autoRespond", autoRespond);
                mentions.addProperty("autoResponseText", autoResponseText);
                mentions.addProperty("autoResponseCooldownSeconds", autoResponseCooldownSeconds);
                JsonObject inventoryButtons = new JsonObject();
                inventoryButtons.addProperty("enabled", inventoryButtonsEnabled);
                data.add("chat", chat);
                data.add("chatInterface", chatInterface);
                data.add("tabs", tabs);
                data.add("notifications", notifications);
                data.add("mentions", mentions);
                data.add("inventoryButtons", inventoryButtons);
                return data;
        }

        /** Writes every present field into the live configuration. */
        public void applyTo(ZeaConfig config) {
                if (multiSendEnabled != null) {
                        config.chat.multiSendEnabled = multiSendEnabled;
                }
                if (dispatchIntervalMillis != null) {
                        config.chat.dispatchIntervalMillis = (int) Math.clamp(dispatchIntervalMillis,
                                        ZeaConfig.MIN_DISPATCH_INTERVAL_MILLIS,
                                        ZeaConfig.MAX_DISPATCH_INTERVAL_MILLIS);
                }
                if (smoothChat != null) {
                        config.chatInterface.smoothChat = smoothChat;
                }
                if (smoothChatDurationMillis != null) {
                        config.chatInterface.smoothChatDurationMillis = (int) Math.clamp(
                                        smoothChatDurationMillis,
                                        ZeaConfig.MIN_SMOOTH_CHAT_MILLIS, ZeaConfig.MAX_SMOOTH_CHAT_MILLIS);
                }
                if (tabDiscord != null) {
                        config.tabs.discord = tabDiscord;
                }
                if (tabVotes != null) {
                        config.tabs.votes = tabVotes;
                }
                if (tabCrates != null) {
                        config.tabs.crates = tabCrates;
                }
                if (tabDeaths != null) {
                        config.tabs.deaths = tabDeaths;
                }
                if (tabSystem != null) {
                        config.tabs.system = tabSystem;
                }
                if (tabUnclassified != null) {
                        config.tabs.unclassified = tabUnclassified;
                }
                if (pmNotifications != null) {
                        config.notifications.pmNotifications = pmNotifications;
                }
                if (pmSound != null) {
                        config.notifications.pmSound = pmSound;
                }
                if (pmDestination != null) {
                        config.notifications.pmDestination = pmDestination;
                }
                if (mentionNotifications != null) {
                        config.notifications.mentionNotifications = mentionNotifications;
                }
                if (mentionSound != null) {
                        config.notifications.mentionSound = mentionSound;
                }
                if (mentionDestination != null) {
                        config.notifications.mentionDestination = mentionDestination;
                }
                if (mentionHighlight != null) {
                        config.mentions.highlight = mentionHighlight;
                }
                if (autoRespond != null) {
                        config.mentions.autoRespond = autoRespond;
                }
                if (autoResponseText != null) {
                        config.mentions.autoResponseText = autoResponseText;
                }
                if (autoResponseCooldownSeconds != null) {
                        config.mentions.autoResponseCooldownSeconds = (int) Math.clamp(
                                        autoResponseCooldownSeconds,
                                        ZeaConfig.MIN_AUTO_RESPONSE_COOLDOWN_SECONDS,
                                        ZeaConfig.MAX_AUTO_RESPONSE_COOLDOWN_SECONDS);
                }
                if (inventoryButtonsEnabled != null) {
                        config.inventoryButtons.enabled = inventoryButtonsEnabled;
                }
        }

        @Override
        public String summary() {
                List<String> groups = new ArrayList<>();
                if (multiSendEnabled != null || dispatchIntervalMillis != null) {
                        groups.add("chat");
                }
                if (smoothChat != null || smoothChatDurationMillis != null) {
                        groups.add("chat interface");
                }
                if (tabDiscord != null || tabVotes != null || tabCrates != null || tabDeaths != null
                                || tabSystem != null || tabUnclassified != null) {
                        groups.add("tabs");
                }
                if (pmNotifications != null || pmSound != null || pmDestination != null
                                || mentionNotifications != null || mentionSound != null
                                || mentionDestination != null) {
                        groups.add("notifications");
                }
                if (mentionHighlight != null || autoRespond != null || autoResponseText != null
                                || autoResponseCooldownSeconds != null) {
                        groups.add("mentions");
                }
                if (inventoryButtonsEnabled != null) {
                        groups.add("inventory buttons");
                }
                return groups.isEmpty() ? "An empty configuration"
                                : "Settings for " + String.join(", ", groups);
        }

        private static Boolean bool(JsonObject data, String group, String field) throws ShareException {
                JsonElement element = field(data, group, field);
                if (element == null) {
                        return null;
                }
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                        throw new ShareException("The value for '" + path(group, field) + "' is not true or false");
                }
                return element.getAsBoolean();
        }

        private static Integer wholeNumber(JsonObject data, String group, String field) throws ShareException {
                JsonElement element = field(data, group, field);
                if (element == null) {
                        return null;
                }
                try {
                        return element.getAsInt();
                } catch (NumberFormatException | IllegalStateException e) {
                        throw new ShareException("The value for '" + path(group, field) + "' is not a number");
                }
        }

        private static Notification.Destination destination(JsonObject data, String group, String field)
                        throws ShareException {
                JsonElement element = field(data, group, field);
                if (element == null) {
                        return null;
                }
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        throw new ShareException("The value for '" + path(group, field) + "' is not a name");
                }
                String name = element.getAsString();
                try {
                        return Notification.Destination.valueOf(name);
                } catch (IllegalArgumentException e) {
                        throw new ShareException("'" + name + "' is not a placement ZEA knows ("
                                        + path(group, field) + ")");
                }
        }

        private static String text(JsonObject data, String group, String field) {
                JsonElement element = field(data, group, field);
                if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        return null;
                }
                StringBuilder cleaned = new StringBuilder();
                element.getAsString().chars()
                                .filter(c -> !Character.isISOControl(c))
                                .limit(MAX_AUTO_RESPONSE_LENGTH)
                                .forEach(cleaned::appendCodePoint);
                return cleaned.toString().strip();
        }

        private static JsonElement field(JsonObject data, String group, String field) {
                JsonElement groupElement = data.get(group);
                if (groupElement == null || !groupElement.isJsonObject()) {
                        return null;
                }
                JsonElement element = groupElement.getAsJsonObject().get(field);
                return element == null || element.isJsonNull() ? null : element;
        }

        private static void addDestination(JsonObject group, String name, Notification.Destination value) {
                if (value != null) {
                        group.addProperty(name, value.name());
                }
        }

        private static String path(String group, String field) {
                return group + "." + field;
        }
}
