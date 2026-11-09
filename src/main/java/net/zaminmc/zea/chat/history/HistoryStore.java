package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.persistence.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads and saves one server's chat history through the safe-write
 * protocol: serialize in memory, verify the round-trip, atomically
 * replace. A corrupt file is preserved as {@code *.corrupted} and an
 * empty history is used instead — damaged history never crashes the mod.
 *
 * <p>Per-entry policy mirrors the inventory buttons: a hand-edited file
 * may contain one damaged entry. {@link EntryWire} tolerates absent
 * fields, entries without a timestamp or any text are dropped with a
 * warning, unknown category names degrade to {@code UNCLASSIFIED}, and
 * a missing formatted text falls back to the plain text — one bad entry
 * never quarantines the whole file.
 *
 * <p>Schema: {@code {"version": 1, "entries": [{"t": <epochMilli>,
 * "c": <category>, "p": <plain>, "f": <formatted>}, …]}}. Timestamps
 * are epoch milliseconds because Gson has no {@link Instant} adapter;
 * plain and formatted text are both stored because stripping codes
 * back out of formatted text is lossy when message content itself
 * contains a {@code §}.
 */
public final class HistoryStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryStore.class);

	/** Bump when the file shape changes; add the matching migration below. */
	public static final int CURRENT_VERSION = 1;

	private final Path file;
	private final JsonStore json = new JsonStore();

	public HistoryStore(Path file) {
		this.file = file;
	}

	/** The file shape on disk. */
	private record HistoryFile(int version, List<EntryWire> entries) {
		HistoryFile {
			entries = entries == null ? List.of() : entries;
		}
	}

	/** One entry as it appears in the file; every field is optional. */
	private record EntryWire(Long t, String c, String p, String f) {
		HistoryEntry toEntry() {
			return new HistoryEntry(
					Instant.ofEpochMilli(t),
					categoryOf(c),
					p == null ? "" : p,
					f == null || f.isBlank() ? (p == null ? "" : p) : f);
		}

		static EntryWire from(HistoryEntry entry) {
			return new EntryWire(
					entry.receivedAt().toEpochMilli(),
					entry.category().name(),
					entry.plainText(),
					entry.formattedText());
		}

		private static MessageCategory categoryOf(String name) {
			if (name != null) {
				try {
					return MessageCategory.valueOf(name);
				} catch (IllegalArgumentException ignored) {
					// A category renamed between versions degrades, never crashes.
				}
			}
			return MessageCategory.UNCLASSIFIED;
		}
	}

	/**
	 * Loads the persisted history, at most {@code limit} newest entries.
	 * The bound exists so a hand-grown file can never pull an unlimited
	 * amount of data into memory — normal files are already bounded by
	 * the retention the writes enforce.
	 */
	public List<HistoryEntry> load(int limit) {
		HistoryFile data = json.load(file, HistoryFile.class,
				() -> new HistoryFile(CURRENT_VERSION, List.of()));
		if (data.version() < 1) {
			LOGGER.warn("History file carries no version; treating it as version 1");
		} else if (data.version() < CURRENT_VERSION) {
			LOGGER.info("Migrating chat history from version {}", data.version());
			// switch (data.version()) { case 1: ... } — added when needed.
		}
		List<HistoryEntry> entries = new ArrayList<>(data.entries().size());
		int dropped = 0;
		for (EntryWire wire : data.entries()) {
			if (wire.t() == null || (isBlank(wire.p()) && isBlank(wire.f()))) {
				dropped++;
				continue;
			}
			entries.add(wire.toEntry());
		}
		if (dropped > 0) {
			LOGGER.warn("Dropped {} unusable history entries from {}", dropped, file);
		}
		entries.sort(Comparator.comparing(HistoryEntry::receivedAt));
		if (entries.size() > limit) {
			entries = new ArrayList<>(entries.subList(entries.size() - limit, entries.size()));
		}
		return List.copyOf(entries);
	}

	/** Saves the history entries in the given (chronological) order. */
	public void save(List<HistoryEntry> entries) {
		List<EntryWire> wire = (entries == null ? List.<HistoryEntry>of() : entries).stream()
				.map(EntryWire::from)
				.toList();
		try {
			json.save(file, new HistoryFile(CURRENT_VERSION, wire));
		} catch (IOException e) {
			LOGGER.warn("Could not save chat history {}", file, e);
		}
	}

	private static boolean isBlank(String text) {
		return text == null || text.isBlank();
	}
}
