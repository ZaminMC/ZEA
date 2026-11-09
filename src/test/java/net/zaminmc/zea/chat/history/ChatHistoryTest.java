package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.chat.model.MessageCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatHistoryTest {
	private static HistoryEntry entry(String text) {
		return entry(text, Instant.EPOCH);
	}

	private static HistoryEntry entry(String text, Instant receivedAt) {
		return new HistoryEntry(receivedAt, MessageCategory.GLOBAL, text, text);
	}

	@Test
	void appendsInOrderAndEvictsBeyondCapacity() {
		ChatHistory history = new ChatHistory(3);
		history.append(entry("one"));
		history.append(entry("two"));
		history.append(entry("three"));
		history.append(entry("four"));

		assertEquals(3, history.size());
		assertEquals(List.of("two", "three", "four"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void capacityBelowOneIsStillAUsableHistory() {
		ChatHistory history = new ChatHistory(0);
		history.append(entry("one"));
		history.append(entry("two"));

		assertEquals(1, history.size());
		assertEquals("two", history.snapshot().getFirst().plainText());
	}

	@Test
	void snapshotsAreIsolatedFromLaterAppends() {
		ChatHistory history = new ChatHistory(10);
		history.append(entry("one"));
		List<HistoryEntry> snapshot = history.snapshot();
		history.append(entry("two"));

		assertEquals(1, snapshot.size(), "a snapshot must not grow afterwards");
		assertEquals(2, history.size());
	}

	@Test
	void prependsPersistedEntriesBeforeSessionEntries() {
		ChatHistory history = new ChatHistory(10);
		history.append(entry("session-1"));
		history.append(entry("session-2"));

		history.prependAll(List.of(entry("old-1"), entry("old-2")));

		assertEquals(List.of("old-1", "old-2", "session-1", "session-2"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void prependingBeyondCapacityDropsTheOldestPersistedEntries() {
		ChatHistory history = new ChatHistory(3);
		history.append(entry("session-1"));

		history.prependAll(List.of(entry("old-1"), entry("old-2"), entry("old-3")));

		assertEquals(List.of("old-2", "old-3", "session-1"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void trimmingRemovesEntriesOlderThanTheCutoff() {
		ChatHistory history = new ChatHistory(10);
		history.append(entry("old", Instant.ofEpochMilli(1_000)));
		history.append(entry("kept", Instant.ofEpochMilli(2_000)));

		int removed = history.trim(Instant.ofEpochMilli(1_500), 10);

		assertEquals(1, removed);
		assertEquals(List.of("kept"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void trimmingKeepsTheNewestEntriesWhenOverTheLimit() {
		ChatHistory history = new ChatHistory(10);
		history.append(entry("one"));
		history.append(entry("two"));
		history.append(entry("three"));

		int removed = history.trim(Instant.EPOCH.minusSeconds(1), 2);

		assertEquals(1, removed);
		assertEquals(List.of("two", "three"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void clearingDropsEverything() {
		ChatHistory history = new ChatHistory(10);
		history.append(entry("one"));
		history.clear();

		assertEquals(0, history.size());
		assertEquals(List.of(), history.snapshot());
	}

	@Test
	void rejectsNullAppend() {
		assertThrows(NullPointerException.class, () -> new ChatHistory(10).append(null));
	}
}
