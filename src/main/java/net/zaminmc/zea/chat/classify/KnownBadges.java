package net.zaminmc.zea.chat.classify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EarthMC badge symbols with verified meanings. These ids mirror the
 * ZEA-REPO {@code datasets/badges.json} entries exactly; ZEA-REPO becomes
 * the runtime source once the repository client exists (see
 * docs/CHAT_CLASSIFICATION.md). The list is not assumed exhaustive —
 * unknown symbols pass through with no invented meaning.
 */
public final class KnownBadges {
	private static final Map<String, String> SYMBOL_TO_ID = Map.ofEntries(
			Map.entry("🌎", "anniversary-globe"),
			Map.entry("❣", "server-heart"),
			Map.entry("+", "supporter"),
			Map.entry("🐇", "rabbit"),
			Map.entry("🔨", "golden-hammer"),
			Map.entry("🐉", "dragon"),
			Map.entry("🎉", "new-player"),
			Map.entry("💎", "top-10-mining"),
			Map.entry("🔥", "snowstorm"));

	/** Longest first, so greedy prefix matching never splits a symbol. */
	private static final List<String> SYMBOLS_LONGEST_FIRST = SYMBOL_TO_ID.keySet().stream()
			.sorted((a, b) -> Integer.compare(b.length(), a.length()))
			.toList();

	private KnownBadges() {
	}

	public static Optional<String> idOf(String symbol) {
		return Optional.ofNullable(SYMBOL_TO_ID.get(symbol));
	}

	/** Known symbols ordered for greedy scanning. */
	public static List<String> symbolsLongestFirst() {
		return SYMBOLS_LONGEST_FIRST;
	}
}
