package net.zaminmc.zea.chat.feeds;

import net.zaminmc.zea.chat.model.WritingChannelReport;

import java.util.Optional;

/**
 * The server's actual writing channel: what the server will write to when
 * the player sends plain chat.
 *
 * <p>Updated <b>only</b> from the server's writing-channel report
 * ({@code You are currently in global (write) and ...}). Never inferred
 * from received messages, never assumed on join (a player does not
 * necessarily start in Global), never changed by selecting a visual tab.
 * Cleared on disconnect.
 */
public final class ChannelState {
	private String writeChannel;

	/** Applies the server's authoritative report. */
	public void applyReport(WritingChannelReport report) {
		if (report == null) {
			throw new NullPointerException("report must not be null");
		}
		writeChannel = report.writeChannel();
	}

	/** Forget the channel on disconnect. */
	public void clear() {
		writeChannel = null;
	}

	/** The current writing channel id, when the server has reported one. */
	public Optional<String> writeChannel() {
		return Optional.ofNullable(writeChannel);
	}
}
