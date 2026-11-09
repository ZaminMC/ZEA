package net.zaminmc.zea.chat.feeds;

/**
 * The multi-send interaction policy: which tab actions and chat sends
 * become multi-target operations. Pure decisions — the chat screen turns
 * the outcomes into feedback, this class never touches UI.
 *
 * <p>The two rules that matter: a feed without a writable server
 * channel (All, PMs, the event feeds) is never a target — All stays
 * read-only — and a slash command typed with targets selected stays
 * vanilla, because a player typing a command means the command.
 */
public final class MultiSendInteraction {
	private MultiSendInteraction() {
	}

	/** What a shift-click on a tab did. */
	public enum ToggleOutcome {
		/** The feed has no writable server channel (All, PMs, events). */
		NO_CHANNEL,
		/** Multi-channel sending is disabled in configuration. */
		DISABLED,
		/** The channel joined the selection. */
		ADDED,
		/** The channel left the selection. */
		REMOVED
	}

	/**
	 * Toggles a feed's channel in the selection. Toggling never changes
	 * the display feed, and never disturbs the selection when the feed
	 * has no channel or sending is disabled.
	 */
	public static ToggleOutcome toggleTarget(SendTargets targets, Feed feed, boolean multiSendEnabled) {
		WriteChannel channel = channelOf(feed);
		if (channel == null) {
			return ToggleOutcome.NO_CHANNEL;
		}
		if (!multiSendEnabled) {
			return ToggleOutcome.DISABLED;
		}
		return targets.toggle(channel) ? ToggleOutcome.ADDED : ToggleOutcome.REMOVED;
	}

	/**
	 * Whether a chat send becomes a multi-target dispatch. Blank input
	 * and slash commands stay vanilla; so does everything while sending
	 * is disabled or no targets are selected.
	 */
	public static boolean shouldIntercept(SendTargets targets, boolean multiSendEnabled, String message) {
		if (!multiSendEnabled || targets.isEmpty() || message == null || message.isBlank()) {
			return false;
		}
		return !message.startsWith("/");
	}

	/** Whether a feed's tab renders as a multi-send target right now. */
	public static boolean isTargeted(SendTargets targets, boolean multiSendEnabled, Feed feed) {
		WriteChannel channel = channelOf(feed);
		return multiSendEnabled && channel != null && targets.isSelected(channel);
	}

	/** The channel a feed would target, or null for feeds without one. */
	public static WriteChannel channelOf(Feed feed) {
		return Channels.byId(feed.id()).orElse(null);
	}
}
