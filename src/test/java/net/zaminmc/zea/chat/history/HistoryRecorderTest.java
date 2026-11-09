package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.earthmc.ServerIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryRecorderTest {
	@TempDir
	Path dir;

	private static final ServerIdentity EARTHMC = new ServerIdentity("earthmc.net");

	/** Wall clock the recorder sees; mutated by tests to advance time. */
	private final long[] now = {0};

	private HistoryRecorder recorder(ChatHistory history, ZeaConfig config, Executor executor) {
		return new HistoryRecorder(history, config, executor, () -> now[0], Runnable::run);
	}

	private static HistoryEntry entry(String text, long epochMilli) {
		return new HistoryEntry(Instant.ofEpochMilli(epochMilli), MessageCategory.GLOBAL, text, text);
	}

	private Path historyFile() {
		return dir.resolve("history").resolve(EARTHMC.fileSafeName() + ".json");
	}

	@Test
	void startSeedsHistoryFromTheServerFile() throws IOException {
		Files.createDirectories(historyFile().getParent());
		new HistoryStore(historyFile()).save(List.of(entry("old", 1_000)));
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), Runnable::run);

		recorder.start(dir.resolve("history"), EARTHMC);

		assertEquals(List.of("old"), history.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void startWithPersistenceDisabledKeepsTheSessionInMemoryOnly() throws IOException {
		Files.createDirectories(historyFile().getParent());
		new HistoryStore(historyFile()).save(List.of(entry("old", 1_000)));
		ZeaConfig config = new ZeaConfig();
		config.history.enabled = false;
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, config, Runnable::run);

		recorder.start(dir.resolve("history"), EARTHMC);
		history.append(entry("session", now[0]));

		assertEquals(List.of("session"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList());
		List<HistoryEntry> onDisk = new HistoryStore(historyFile()).load(100);
		assertEquals(List.of("old"),
				onDisk.stream().map(HistoryEntry::plainText).toList(),
				"a disabled history must not touch any file");
	}

	@Test
	void tickWritesADirtyHistoryOnlyAfterTheFlushInterval() {
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), Runnable::run);
		recorder.start(dir.resolve("history"), EARTHMC);

		history.append(entry("one", now[0]));
		recorder.markDirty();
		now[0] = HistoryRecorder.FLUSH_INTERVAL_MILLIS - 1;
		recorder.tick();
		assertFalse(Files.exists(historyFile()), "nothing is written before the interval elapses");

		now[0] = HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		assertTrue(Files.exists(historyFile()), "a dirty history flushes on schedule");

		List<HistoryEntry> loaded = new HistoryStore(historyFile()).load(100);
		assertEquals(List.of("one"), loaded.stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void cleanHistoriesAreNeverWritten() {
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), Runnable::run);
		recorder.start(dir.resolve("history"), EARTHMC);

		now[0] = HistoryRecorder.FLUSH_INTERVAL_MILLIS * 3;
		recorder.tick();

		assertFalse(Files.exists(historyFile()), "no messages, no writes");
	}

	@Test
	void writesCoalesceWhileOneIsInFlight() {
		Deque<Runnable> queue = new ArrayDeque<>();
		Executor deferring = queue::add;
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), deferring);
		recorder.start(dir.resolve("history"), EARTHMC);
		queue.poll().run(); // the deferred load completes

		history.append(entry("one", now[0]));
		recorder.markDirty();
		now[0] = HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		assertTrue(recorder.writing(), "a write is in flight");

		history.append(entry("two", now[0]));
		recorder.markDirty();
		now[0] += HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		assertEquals(1, queue.size(), "no second write while one is in flight");

		queue.poll().run(); // the in-flight save completes
		assertFalse(recorder.writing());

		now[0] += HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		queue.poll().run(); // the coalesced write lands
		assertTrue(Files.exists(historyFile()));
		List<HistoryEntry> loaded = new HistoryStore(historyFile()).load(100);
		assertEquals(List.of("one", "two"),
				loaded.stream().map(HistoryEntry::plainText).toList(),
				"the coalesced write must carry both messages");
	}

	@Test
	void stopFlushesPendingChangesAndEndsTheSession() {
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), Runnable::run);
		recorder.start(dir.resolve("history"), EARTHMC);

		history.append(entry("last words", now[0]));
		recorder.markDirty();
		recorder.stop();

		assertTrue(Files.exists(historyFile()), "disconnect must flush pending history");
		assertEquals(0, history.size(), "the session ends with the connection");

		// And the next session seeds from what was flushed.
		ChatHistory next = new ChatHistory(10);
		HistoryRecorder nextRecorder = recorder(next, new ZeaConfig(), Runnable::run);
		nextRecorder.start(dir.resolve("history"), EARTHMC);
		assertEquals(List.of("last words"),
				next.snapshot().stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void retentionRemovesExpiredEntriesOnItsSchedule() {
		ZeaConfig config = new ZeaConfig();
		config.history.retention = ZeaConfig.History.Retention.ONE_DAY;
		ChatHistory history = new ChatHistory(100);
		HistoryRecorder recorder = recorder(history, config, Runnable::run);
		recorder.start(dir.resolve("history"), EARTHMC);

		history.append(entry("fresh", now[0]));
		history.append(entry("stale", now[0] - 2L * 86_400_000));
		history.append(entry("ancient", now[0] - 30L * 86_400_000));
		recorder.markDirty();

		now[0] = HistoryRecorder.RETENTION_INTERVAL_MILLIS;
		recorder.tick(); // flushes and runs retention

		assertEquals(List.of("fresh"),
				history.snapshot().stream().map(HistoryEntry::plainText).toList(),
				"entries older than the retention are dropped on schedule");
		List<HistoryEntry> loaded = new HistoryStore(historyFile()).load(100);
		assertEquals(List.of("fresh"),
				loaded.stream().map(HistoryEntry::plainText).toList(),
				"the shrinkage is persisted");
	}

	@Test
	void rejectedWritesAreRetriedOnLaterTicks() {
		List<Boolean> rejections = new ArrayList<>(List.of(false, true));
		Executor flaky = task -> {
			if (!rejections.isEmpty() && rejections.removeFirst()) {
				throw new RejectedExecutionException("busy");
			}
			task.run();
		};
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), flaky);
		recorder.start(dir.resolve("history"), EARTHMC);

		history.append(entry("one", now[0]));
		recorder.markDirty();
		now[0] = HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		assertFalse(Files.exists(historyFile()), "the rejected write must not land");

		now[0] += HistoryRecorder.FLUSH_INTERVAL_MILLIS;
		recorder.tick();
		assertTrue(Files.exists(historyFile()), "a busy executor is retried, not lost");
	}

	@Test
	void startIsolatesHistoryPerServer() {
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), Runnable::run);

		ServerIdentity aurora = new ServerIdentity("aurora.earthmc.net");
		recorder.start(dir.resolve("history"), aurora);
		history.append(entry("aurora chat", now[0]));
		recorder.markDirty();
		recorder.stop();

		recorder.start(dir.resolve("history"), EARTHMC);
		assertEquals(0, history.size(), "another server's history starts empty");
		history.append(entry("main chat", now[0]));
		recorder.markDirty();
		recorder.stop();

		List<HistoryEntry> auroraLoaded = new HistoryStore(
						dir.resolve("history").resolve("aurora.earthmc.net.json")).load(100);
		assertEquals(List.of("aurora chat"),
				auroraLoaded.stream().map(HistoryEntry::plainText).toList());
		List<HistoryEntry> mainLoaded = new HistoryStore(historyFile()).load(100);
		assertEquals(List.of("main chat"),
				mainLoaded.stream().map(HistoryEntry::plainText).toList());
	}

	@Test
	void rejectedLoadLeavesAUsableSession() {
		Executor rejecting = task -> {
			throw new RejectedExecutionException("closing");
		};
		ChatHistory history = new ChatHistory(10);
		HistoryRecorder recorder = recorder(history, new ZeaConfig(), rejecting);

		recorder.start(dir.resolve("history"), EARTHMC);
		history.append(entry("still recorded", now[0]));
		recorder.markDirty();

		assertEquals(1, history.size(), "the session works without the loaded history");
		assertEquals(Optional.of("still recorded"),
				history.snapshot().stream().map(HistoryEntry::plainText).findFirst());
	}
}
