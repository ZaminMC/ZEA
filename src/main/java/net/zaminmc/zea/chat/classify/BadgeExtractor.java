package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.zaminmc.zea.chat.model.Badge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extracts badges from the component region before a message's affiliation
 * bracket.
 *
 * <p>Component-first: EarthMC messages are built from sibling components,
 * and badges carry their own hover/click events, so siblings lying wholly
 * inside the badge region are badges — with their original components
 * preserved. When the message is a single flat component (no per-badge
 * events exist to preserve), known symbols are matched from text instead.
 *
 * <p>A multi-symbol component (several badges sent as one sibling) is kept
 * as a single cluster badge — it preserves exactly what the server sent
 * and gains no invented identity.
 */
public final class BadgeExtractor {
	private BadgeExtractor() {
	}

	/**
	 * @param original        the full message component
	 * @param badgeRegionEnd  offset in the flattened text where the
	 *                        affiliation bracket starts
	 */
	public static List<Badge> extract(Component original, int badgeRegionEnd) {
		String full = original.getString();
		List<Component> siblings = original.getSiblings();

		int siblingChars = 0;
		for (Component sibling : siblings) {
			siblingChars += sibling.getString().length();
		}
		int rootLength = full.length() - siblingChars;

		List<Badge> badges = new ArrayList<>();
		int offset = 0;
		if (rootLength > 0) {
			if (rootLength <= badgeRegionEnd) {
				addComponentBadge(badges, rootComponent(original, full, rootLength));
			}
			offset = rootLength;
		}
		for (Component sibling : siblings) {
			if (offset >= badgeRegionEnd) {
				break;
			}
			int end = offset + sibling.getString().length();
			if (end <= badgeRegionEnd) {
				addComponentBadge(badges, sibling);
			}
			offset = end;
		}
		if (!badges.isEmpty()) {
			return badges;
		}
		return fromText(full.substring(0, Math.min(badgeRegionEnd, full.length())));
	}

	/** A component representing only the root's own text (the root minus siblings). */
	private static Component rootComponent(Component original, String full, int rootLength) {
		MutableComponent root = Component.literal(full.substring(0, rootLength));
		root.setStyle(original.getStyle());
		return root;
	}

	private static void addComponentBadge(List<Badge> badges, Component component) {
		String symbol = component.getString().trim();
		if (!symbol.isEmpty()) {
			badges.add(new Badge(Optional.of(component), symbol, KnownBadges.idOf(symbol)));
		}
	}

	/** Text fallback: greedy-match known symbols; unknown text yields nothing. */
	private static List<Badge> fromText(String prefix) {
		List<Badge> badges = new ArrayList<>();
		String rest = prefix.trim();
		while (!rest.isEmpty()) {
			String symbol = firstKnownSymbol(rest);
			if (symbol == null) {
				break;
			}
			badges.add(new Badge(Optional.empty(), symbol, KnownBadges.idOf(symbol)));
			rest = rest.substring(symbol.length()).trim();
		}
		return badges;
	}

	private static String firstKnownSymbol(String text) {
		for (String symbol : KnownBadges.symbolsLongestFirst()) {
			if (text.startsWith(symbol)) {
				return symbol;
			}
		}
		return null;
	}
}
