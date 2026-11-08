package net.zaminmc.zea.chat.model;

/**
 * The server's report of the player's current writing channel, e.g.
 * {@code You are currently in global (write) and 5 other channels}.
 *
 * <p>This response is the source of truth for the server-side writing
 * channel (Stage 3). It is never inferred from received messages.
 *
 * @param writeChannel  channel the server will write to
 * @param otherChannels number of other channels mentioned
 */
public record WritingChannelReport(String writeChannel, int otherChannels) implements MessagePayload {
	public WritingChannelReport {
		if (writeChannel == null) {
			throw new NullPointerException("writeChannel must not be null");
		}
		if (writeChannel.isBlank()) {
			throw new IllegalArgumentException("writeChannel must not be blank");
		}
		if (otherChannels < 0) {
			throw new IllegalArgumentException("otherChannels must not be negative");
		}
	}
}
