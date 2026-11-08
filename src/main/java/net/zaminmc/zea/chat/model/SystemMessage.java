package net.zaminmc.zea.chat.model;

/**
 * A server system message prefixed with {@code EarthMC »}, including AFK
 * notifications.
 *
 * @param content text following the prefix
 */
public record SystemMessage(String content) implements MessagePayload {
	public SystemMessage {
		if (content == null) {
			throw new NullPointerException("content must not be null");
		}
	}
}
