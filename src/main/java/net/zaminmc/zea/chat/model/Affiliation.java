package net.zaminmc.zea.chat.model;

import java.util.Optional;

/**
 * The {@code [Nation|Town]} segment of a player message.
 *
 * <p>Nation and town are stored separately. The town is not always present;
 * a single-segment bracket keeps the raw value without guessing whether it
 * names a nation or a town — that distinction needs observed data or an
 * API lookup (Stage 4). Original colors live in the original component.
 *
 * @param raw    the segment content without brackets
 * @param nation nation name, when the {@code [nation|town]} form was present
 * @param town   town name, when the {@code [nation|town]} form was present
 */
public record Affiliation(String raw, Optional<String> nation, Optional<String> town) {
	public Affiliation {
		if (raw == null || nation == null || town == null) {
			throw new NullPointerException("affiliation fields must not be null");
		}
		if (raw.isBlank()) {
			throw new IllegalArgumentException("affiliation raw must not be blank");
		}
	}

	/** Parses {@code [nation|town]} (both present) or keeps a single segment raw. */
	public static Affiliation parse(String raw) {
		int split = raw.indexOf('|');
		if (split < 0) {
			return new Affiliation(raw, Optional.empty(), Optional.empty());
		}
		String nation = raw.substring(0, split);
		String town = raw.substring(split + 1);
		if (nation.isBlank() || town.isBlank()) {
			return new Affiliation(raw, Optional.empty(), Optional.empty());
		}
		return new Affiliation(raw, Optional.of(nation), Optional.of(town));
	}
}
