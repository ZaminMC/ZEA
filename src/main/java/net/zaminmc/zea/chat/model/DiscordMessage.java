package net.zaminmc.zea.chat.model;

/**
 * A message forwarded from Discord. EarthMC prefixes these with
 * {@code [Discord]}; players reply through normal in-game chat. There is
 * no Discord writing channel — this is an incoming category only.
 *
 * <p>The remainder after the prefix is kept raw: its internal structure
 * (author, content) is not yet verified against observed messages.
 *
 * @param remainder text following the {@code [Discord]} prefix
 */
public record DiscordMessage(String remainder) implements MessagePayload {
	public DiscordMessage {
		if (remainder == null) {
			throw new NullPointerException("remainder must not be null");
		}
	}
}
