package net.zaminmc.zea.chat.model;

/**
 * A private message, incoming or outgoing.
 *
 * @param sender    sending player name
 * @param recipient receiving player name
 * @param direction whether this message is incoming ("to me") or outgoing ("from me")
 * @param content   message body
 */
public record PrivateMessage(String sender, String recipient, Direction direction, String content)
		implements MessagePayload {
	public PrivateMessage {
		if (sender == null || recipient == null || direction == null || content == null) {
			throw new NullPointerException("private message fields must not be null");
		}
		if (sender.isBlank() || recipient.isBlank()) {
			throw new IllegalArgumentException("private message names must not be blank");
		}
	}

	public enum Direction {
		INCOMING,
		OUTGOING
	}
}
