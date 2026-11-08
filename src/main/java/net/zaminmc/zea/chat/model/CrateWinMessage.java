package net.zaminmc.zea.chat.model;

/**
 * A crate win notification, e.g. {@code akirarocks won 16 gold from a bruh
 * box}. Amounts, currencies and crate names are parsed, never hardcoded.
 *
 * @param player    winning player name
 * @param amount    reward amount
 * @param currency  reward currency as written ("gold")
 * @param crateName crate name as written ("gold crate", "bruh box")
 */
public record CrateWinMessage(String player, int amount, String currency, String crateName) implements MessagePayload {
	public CrateWinMessage {
		if (player == null || currency == null || crateName == null) {
			throw new NullPointerException("crate win fields must not be null");
		}
		if (player.isBlank() || currency.isBlank() || crateName.isBlank()) {
			throw new IllegalArgumentException("crate win fields must not be blank");
		}
		if (amount <= 0) {
			throw new IllegalArgumentException("crate win amount must be positive");
		}
	}
}
