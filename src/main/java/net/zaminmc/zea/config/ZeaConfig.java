package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder;
import net.zaminmc.zea.chat.feeds.Feeds;
import net.zaminmc.zea.notify.Notification;

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

	/** Auto-response cooldown slider bounds, in seconds. */
	public static final int MIN_AUTO_RESPONSE_COOLDOWN_SECONDS = 10;
	public static final int MAX_AUTO_RESPONSE_COOLDOWN_SECONDS = 300;

	/** History size slider bounds, in entries per server. */
	public static final int MIN_HISTORY_ENTRIES = 500;
	public static final int MAX_HISTORY_ENTRIES = 10_000;

	/** The default history size, deliberately far below the maximum. */
	public static final int DEFAULT_HISTORY_ENTRIES = 2_000;

	/** Inventory buttons beside container GUIs. */
	public static final class InventoryButtons {
		@ConfigOption(name = "Inventory buttons", desc = "Show your custom buttons beside "
				+ "inventory and chest screens on EarthMC. Buttons run the command "
				+ "you configured when clicked.")
		@ConfigEditorBoolean
		@ConfigOrder(0)
		public boolean enabled = true;
	}

	/** Persistent chat history. */
	public static final class History {
		/** Retention choices for persistent chat history. */
		public enum Retention {
			ONE_DAY("1 day", 1),
			THREE_DAYS("3 days", 3),
			SEVEN_DAYS("1 week", 7),
			FOURTEEN_DAYS("2 weeks", 14),
			THIRTY_DAYS("30 days", 30),
			NINETY_DAYS("90 days", 90);

			private final String label;
			private final int days;

			Retention(String label, int days) {
				this.label = label;
				this.days = days;
			}

			/** What the MoulConfig dropdown shows for this choice. */
			@Override
			public String toString() {
				return label;
			}

			/** The retention length in whole days. */
			public int days() {
				return days;
			}
		}

		@ConfigOption(name = "Persistent history", desc = "Remember EarthMC chat between "
				+ "sessions. History is stored per server, only on this computer, "
				+ "and is never uploaded.")
		@ConfigEditorBoolean
		@ConfigOrder(0)
		public boolean enabled = true;

		@ConfigOption(name = "History retention", desc = "How long messages survive. Older "
				+ "messages are removed by the next cleanup — history never grows "
				+ "without bound.")
		@ConfigEditorDropdown
		@ConfigOrder(1)
		public Retention retention = Retention.SEVEN_DAYS;

		@ConfigOption(name = "History size", desc = "How many messages are kept per "
				+ "server. Searching covers exactly these.")
		@ConfigEditorSlider(minValue = MIN_HISTORY_ENTRIES, maxValue = MAX_HISTORY_ENTRIES,
				minStep = 500)
		@ConfigOrder(2)
		public int maxEntries = DEFAULT_HISTORY_ENTRIES;
	}

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

	/** Notifications about private messages and mentions. */
	public static final class Notifications {
		@ConfigOption(name = "PM notifications", desc = "Show a notification when a private "
				+ "message arrives. It stays until you reply to that player.")
		@ConfigEditorBoolean
		@ConfigOrder(0)
		public boolean pmNotifications = true;

		@ConfigOption(name = "PM notification sound", desc = "Play a soft cue when a private "
				+ "message arrives.")
		@ConfigEditorBoolean
		@ConfigOrder(1)
		public boolean pmSound = true;

		@ConfigOption(name = "PM notification placement", desc = "Where private message "
				+ "notifications appear. The action bar is the calmer choice; titles "
				+ "re-show while the message waits for a reply.")
		@ConfigEditorDropdown
		@ConfigOrder(2)
		public Notification.Destination pmDestination = Notification.Destination.ACTION_BAR;

		@ConfigOption(name = "Mention notifications", desc = "Show a short notification when "
				+ "someone mentions your name in chat.")
		@ConfigEditorBoolean
		@ConfigOrder(3)
		public boolean mentionNotifications = true;

		@ConfigOption(name = "Mention notification sound", desc = "Play a soft cue when "
				+ "someone mentions your name in chat.")
		@ConfigEditorBoolean
		@ConfigOrder(4)
		public boolean mentionSound = true;

		@ConfigOption(name = "Mention notification placement", desc = "Where mention "
				+ "notifications appear.")
		@ConfigEditorDropdown
		@ConfigOrder(5)
		public Notification.Destination mentionDestination = Notification.Destination.ACTION_BAR;
	}

	/** Mentions: chat highlighting and the opt-in auto-response. */
	public static final class Mentions {
		@ConfigOption(name = "Highlight mentioned messages", desc = "Tint the background of "
				+ "chat lines that mention your name, so they stand out while "
				+ "scrolling.")
		@ConfigEditorBoolean
		@ConfigOrder(0)
		public boolean highlight = true;

		@ConfigOption(name = "Auto-respond to mentions", desc = "Automatically send the "
				+ "response below when someone mentions you. Off by default; "
				+ "cooldown-limited and capped per session.")
		@ConfigEditorBoolean
		@ConfigOrder(1)
		public boolean autoRespond = false;

		@ConfigOption(name = "Auto-response message", desc = "What to send when the "
				+ "auto-response triggers. Keep it short, and do not include "
				+ "other players' names — the response is public chat.")
		@ConfigEditorText(forbidden = "")
		@ConfigOrder(2)
		public String autoResponseText = "";

		@ConfigOption(name = "Auto-response cooldown", desc = "Minimum seconds between "
				+ "two automatic responses.")
		@ConfigEditorSlider(minValue = MIN_AUTO_RESPONSE_COOLDOWN_SECONDS,
				maxValue = MAX_AUTO_RESPONSE_COOLDOWN_SECONDS, minStep = 5)
		@ConfigOrder(3)
		public int autoResponseCooldownSeconds = 30;
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

	@Category(name = "Notifications", desc = "Private message and mention notifications.")
	@ConfigOrder(3)
	public Notifications notifications = new Notifications();

	@Category(name = "Mentions", desc = "How mentions of your name are handled in chat.")
	@ConfigOrder(4)
	public Mentions mentions = new Mentions();

	@Category(name = "Inventory buttons", desc = "Custom buttons beside container screens.")
	@ConfigOrder(5)
	public InventoryButtons inventoryButtons = new InventoryButtons();

	@Category(name = "Chat history", desc = "Persistent, searchable EarthMC chat history.")
	@ConfigOrder(6)
	public History history = new History();

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

	/** Whether private message notifications are enabled; on by default. */
	public boolean pmNotificationsEnabled() {
		return notifications.pmNotifications;
	}

	/** Whether the private message cue sounds; on by default. */
	public boolean pmSoundEnabled() {
		return notifications.pmSound;
	}

	/** Where private message notifications appear; defaults if unset. */
	public Notification.Destination pmDestination() {
		return notifications.pmDestination == null
				? Notification.Destination.ACTION_BAR
				: notifications.pmDestination;
	}

	/** Whether mention notifications are enabled; on by default. */
	public boolean mentionNotificationsEnabled() {
		return notifications.mentionNotifications;
	}

	/** Whether the mention cue sounds; on by default. */
	public boolean mentionSoundEnabled() {
		return notifications.mentionSound;
	}

	/** Where mention notifications appear; defaults if unset. */
	public Notification.Destination mentionDestination() {
		return notifications.mentionDestination == null
				? Notification.Destination.ACTION_BAR
				: notifications.mentionDestination;
	}

	/** Whether mentioned chat lines are tinted; on by default. */
	public boolean mentionHighlightEnabled() {
		return mentions.highlight;
	}

	/** Whether the mention auto-response is enabled; off by default. */
	public boolean autoRespondEnabled() {
		return mentions.autoRespond;
	}

	/** The configured auto-response text, trimmed. */
	public String autoResponseText() {
		return mentions.autoResponseText == null ? "" : mentions.autoResponseText.strip();
	}

	/** The auto-response cooldown, clamped to the slider range. */
	public long autoResponseCooldownMillis() {
		return Math.clamp(mentions.autoResponseCooldownSeconds,
				MIN_AUTO_RESPONSE_COOLDOWN_SECONDS, MAX_AUTO_RESPONSE_COOLDOWN_SECONDS) * 1000L;
	}

	/** Whether inventory buttons render; on by default (they do nothing until configured). */
	public boolean inventoryButtonsEnabled() {
		return inventoryButtons.enabled;
	}

	/** Whether chat history persists between sessions; on by default, strictly retention-bound. */
	public boolean historyEnabled() {
		return history.enabled;
	}

	/** The selected retention, in days; a hand-cleared field falls back to one week. */
	public int historyRetentionDays() {
		History.Retention retention = history.retention;
		return (retention == null ? History.Retention.SEVEN_DAYS : retention).days();
	}

	/** The selected retention, in milliseconds. */
	public long historyRetentionMillis() {
		return historyRetentionDays() * 86_400_000L;
	}

	/** The history size limit, clamped to the slider range. */
	public int historyMaxEntries() {
		return (int) Math.clamp(history.maxEntries, MIN_HISTORY_ENTRIES, MAX_HISTORY_ENTRIES);
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
