package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.chat.model.MessageCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryStoreTest {
	@TempDir
	Path dir;

	private static HistoryEntry entry(String text, long epochMilli) {
		return new HistoryEntry(Instant.ofEpochMilli(epochMilli), MessageCategory.GLOBAL, text,
				"§7" + text);
	}

	@Test
	void roundTripsEntriesWithFormattingAndOrder() {
		Path file = dir.resolve("history.json");
		HistoryStore store = new HistoryStore(file);
		store.save(List.of(entry("one", 1_000), entry("two", 2_000)));

		List<HistoryEntry> loaded = store.load(100);

		assertEquals(2, loaded.size());
		assertEquals("one", loaded.get(0).plainText());
		assertEquals("§7one", loaded.get(0).formattedText(), "legacy codes must survive the round-trip");
		assertEquals(Instant.ofEpochMilli(1_000), loaded.get(0).receivedAt());
		assertEquals(MessageCategory.GLOBAL, loaded.get(0).category());
	}

	@Test
	void writesTheSchemaVersion() throws IOException {
		Path file = dir.resolve("history.json");
		new HistoryStore(file).save(List.of(entry("one", 1_000)));

		assertTrue(Files.readString(file).contains("\"version\": 1"), "schema version must be on disk");
	}

	@Test
	void missingFileYieldsAnEmptyHistory() {
		List<HistoryEntry> loaded = new HistoryStore(dir.resolve("absent.json")).load(100);

		assertEquals(List.of(), loaded);
	}

	@Test
	void corruptFileFallsBackToEmptyAndIsPreserved() throws IOException {
		Path file = dir.resolve("history.json");
		Files.writeString(file, "{ this is not json");

		List<HistoryEntry> loaded = new HistoryStore(file).load(100);

		assertEquals(List.of(), loaded);
		assertTrue(Files.exists(dir.resolve("history.json.corrupted")),
				"the corrupt file must be preserved for inspection");
	}

	@Test
	void oneDamagedEntryDoesNotTakeTheFileDown() throws IOException {
		Path file = dir.resolve("history.json");
		Files.writeString(file, """
                                                {"version": 1, "entries": [
                                                        {"t": 1000, "c": "GLOBAL", "p": "kept", "f": "§7kept"},
                                                        {"c": "GLOBAL", "p": "no timestamp"},
                                                        {"t": 3000, "c": "GLOBAL"},
                                                        {"t": 4000, "c": "NOT_A_CATEGORY", "p": "degraded", "f": "§7degraded"}
                                                ]}
                                                """);

		List<HistoryEntry> loaded = new HistoryStore(file).load(100);

		assertEquals(2, loaded.size(), "entry without timestamp and entry without text are dropped");
		assertEquals("kept", loaded.get(0).plainText());
		assertEquals(MessageCategory.UNCLASSIFIED, loaded.get(1).category(),
				"an unknown category degrades to unclassified");
		assertEquals("§7degraded", loaded.get(1).formattedText());
	}

	@Test
	void missingFormattedTextFallsBackToPlain() throws IOException {
		Path file = dir.resolve("history.json");
		Files.writeString(file,
				"{\"version\": 1, \"entries\": [{\"t\": 1000, \"c\": \"VOTE\", \"p\": \"plain only\"}]}");

		List<HistoryEntry> loaded = new HistoryStore(file).load(100);

		assertEquals(1, loaded.size());
		assertEquals("plain only", loaded.get(0).formattedText());
		assertEquals(MessageCategory.VOTE, loaded.get(0).category());
	}

	@Test
	void unorderedFilesAreSortedChronologically() throws IOException {
		Path file = dir.resolve("history.json");
		Files.writeString(file, """
                                                {"version": 1, "entries": [
                                                        {"t": 3000, "c": "GLOBAL", "p": "third"},
                                                        {"t": 1000, "c": "GLOBAL", "p": "first"},
                                                        {"t": 2000, "c": "GLOBAL", "p": "second"}
                                                ]}
                                                """);

		List<HistoryEntry> loaded = new HistoryStore(file).load(100);

		assertEquals(List.of("first", "second", "third"),
				loaded.stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void loadKeepsOnlyTheNewestEntriesUpToTheLimit() throws IOException {
		Path file = dir.resolve("history.json");
		new HistoryStore(file).save(List.of(entry("one", 1_000), entry("two", 2_000), entry("three", 3_000)));

		List<HistoryEntry> loaded = new HistoryStore(file).load(2);

		assertEquals(List.of("two", "three"),
				loaded.stream().map(HistoryEntry::plainText).toList(),
				"an oversized file must not pull everything into memory");
	}
}
