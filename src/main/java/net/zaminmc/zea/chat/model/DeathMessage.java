package net.zaminmc.zea.chat.model;

/**
 * A death notification, e.g. {@code ☠ xfw_x was killed by dex247}.
 * Combat details (damage hover) are preserved on the original component
 * attached to the enclosing {@link ClassifiedMessage}.
 *
 * @param victim the player who died
 * @param killer the player responsible
 */
public record DeathMessage(String victim, String killer) implements MessagePayload {
	public DeathMessage {
		if (victim == null || killer == null) {
			throw new NullPointerException("death message fields must not be null");
		}
		if (victim.isBlank() || killer.isBlank()) {
			throw new IllegalArgumentException("death message fields must not be blank");
		}
	}
}
