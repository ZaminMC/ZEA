package net.zaminmc.zea.chat.model;

/**
 * A vote notification, e.g. {@code AdamGladio voted and received a gold
 * crate /vote}.
 *
 * @param player voting player name
 * @param reward reward phrase as written ("gold crate")
 */
public record VoteMessage(String player, String reward) implements MessagePayload {
	public VoteMessage {
		if (player == null || reward == null) {
			throw new NullPointerException("vote message fields must not be null");
		}
		if (player.isBlank() || reward.isBlank()) {
			throw new IllegalArgumentException("vote message fields must not be blank");
		}
	}
}
