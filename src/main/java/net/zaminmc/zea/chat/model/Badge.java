package net.zaminmc.zea.chat.model;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * A chat badge preceding a player's message.
 *
 * <p>Preserves the original symbol, its display style (including hover and
 * click events, e.g. the Snowstorm badge's store link) and — when the
 * symbol is one of the verified EarthMC badges — its semantic identity.
 *
 * <p>{@code original} is empty when the badge was recognized from flattened
 * text only (flat components cannot carry per-badge events, so there is
 * nothing to preserve beyond the symbol itself).
 *
 * @param original the component this badge was extracted from, if any
 * @param symbol   the badge as it appears in chat
 * @param knownId  verified semantic identity, when known
 */
public record Badge(Optional<Component> original, String symbol, Optional<String> knownId) {
	public Badge {
		if (original == null || symbol == null || knownId == null) {
			throw new NullPointerException("badge fields must not be null");
		}
		if (symbol.isBlank()) {
			throw new IllegalArgumentException("badge symbol must not be blank");
		}
	}
}
