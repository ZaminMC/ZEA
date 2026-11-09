package net.zaminmc.zea.chat.capture;

import com.google.gson.JsonElement;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.persistence.JsonStore;
import net.zaminmc.zea.util.LegacyText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Records the vanilla JSON form of incoming EarthMC messages to a local
 * file, on explicit user request — the ground truth for component-level
 * fixtures, badge hover data and nickname styling, none of which client
 * logs can show. Capture is a diagnostic tap, not a feature path: it is
 * off unless turned on, bounded, and never uploads anywhere.
 *
 * <p>Threading contract, mirroring {@code HistoryRecorder}: all state
 * methods are confined to the client thread. The component snapshot is
 * taken there (component trees are live runtime objects); file IO runs
 * on the sequential executor, and the completion flag marshals back
 * through {@code toClientThread}. Intermediate flushes coalesce through
 * that flag — while one write is in flight the next is skipped — but the
 * final write of a session is always attempted.
 *
 * <p>File schema: {@code {"format": "zea-capture", "version": 1,
 * "startedAt": <millis>, "stoppedAt": <millis>, "stoppedReason":
 * <EXPLICIT|CAP|SHUTDOWN>, "entries": [{"t": <millis>, "category":
 * <name>, "text": <plain>, "legacy": <§-coded>, "component": <vanilla
 * component JSON>, "error": <why component is missing>}, …]}}. While a
 * capture is running the file is a valid snapshot without
 * {@code stoppedAt}; Gson omits null fields.
 */
public final class CaptureRecorder {
	private static final Logger LOGGER = LoggerFactory.getLogger(CaptureRecorder.class);

	/** Session capacity — enough for every fixture family, bounded on disk. */
	public static final int MAX_ENTRIES = 100;

	/** Entries between intermediate flushes (the stop write always lands). */
	private static final int FLUSH_INTERVAL = 25;

	/** The capture file schema version; bump with a migration when it changes. */
	public static final int CURRENT_VERSION = 1;

	private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	/** Why a capture session ended. */
	public enum StopReason {EXPLICIT, CAP, SHUTDOWN}

	/** Live state, for {@link #status}. */
	public record Status(boolean active, int recorded, int capacity, Path file) {
	}

	/** A finished session, for feedback. */
	public record Stopped(int recorded, Path file, StopReason reason) {
	}

	/** Turns a component into its serialized form; never throws. */
	@FunctionalInterface
	public interface Serializer {
		ComponentJson.Result serialize(net.minecraft.network.chat.Component component);
	}

	private final Path directory;
	private final Serializer serializer;
	private final Executor executor;
	private final Consumer<Runnable> toClientThread;
	private final Consumer<Stopped> onAutoStop;
	private final JsonStore json = new JsonStore();

	private Session session;
	private boolean writing;
	private String lastStamp = "";
	private int stampCounter;

	/**
	 * @param directory     where capture files are written
	 * @param serializer    component serialization, injected for tests
	 * @param executor      sequential executor for file IO
	 * @param toClientThread marshals the completion flag back to the client thread
	 * @param onAutoStop    told on the client thread when the cap ends a session
	 */
	public CaptureRecorder(Path directory, Serializer serializer, Executor executor,
			Consumer<Runnable> toClientThread, Consumer<Stopped> onAutoStop) {
		this.directory = directory;
		this.serializer = serializer;
		this.executor = executor;
		this.toClientThread = toClientThread;
		this.onAutoStop = onAutoStop;
	}

	/**
	 * Begins a capture session. Returns the file messages will be written
	 * to, or null when one is already running — starting never disturbs a
	 * live session.
	 */
	public Path start() {
		if (session != null) {
			return null;
		}
		Path file = nextFile();
		session = new Session(file, Instant.now());
		return file;
	}

	/** Ends the session and returns what it captured, or null if none. */
	public Stopped stop() {
		return endSession(StopReason.EXPLICIT);
	}

	/** Ends the session, if any, without feedback — for client shutdown. */
	public void shutdown() {
		endSession(StopReason.SHUTDOWN);
	}

	/** Writes the session out now if one is running and no write is in flight. */
	public void flush() {
		Session current = session;
		if (current == null || writing) {
			return;
		}
		submitWrite(current, null, null);
	}

	/** Current state, for the status command. */
	public Status status() {
		if (session == null) {
			return new Status(false, 0, MAX_ENTRIES, null);
		}
		return new Status(true, session.entries.size(), MAX_ENTRIES, session.file);
	}

	/** Records one classified message, or stops the session at the cap. */
	public void onClassified(ClassifiedMessage message) {
		Session current = session;
		if (current == null || message.captured().text().isBlank()) {
			return;
		}
		ComponentJson.Result serialized = serializer.serialize(message.captured().original());
		current.entries.add(new EntryWire(
				message.captured().receivedAt().toEpochMilli(),
				message.category().name(),
				message.captured().text(),
				LegacyText.format(message.captured().original()),
				serialized.json(),
				serialized.error()));
		if (current.entries.size() >= MAX_ENTRIES) {
			Stopped stopped = endSession(StopReason.CAP);
			if (stopped != null) {
				onAutoStop.accept(stopped);
			}
		} else if (current.entries.size() % FLUSH_INTERVAL == 0) {
			flush();
		}
	}

	private Stopped endSession(StopReason reason) {
		Session current = session;
		if (current == null) {
			return null;
		}
		session = null;
		submitWrite(current, Instant.now(), reason);
		return new Stopped(current.entries.size(), current.file, reason);
	}

	private void submitWrite(Session current, Instant stoppedAt, StopReason reason) {
		List<EntryWire> snapshot = List.copyOf(current.entries);
		writing = true;
		boolean queued = submit(() -> {
			try {
				writeFile(current.file, current.startedAt, stoppedAt, reason, snapshot);
			} finally {
				toClientThread.accept(() -> writing = false);
			}
		});
		if (!queued) {
			writing = false;
			LOGGER.warn("Capture executor rejected a write; {} entries may be missing from {}",
					snapshot.size(), current.file);
		}
	}

	private boolean submit(Runnable task) {
		try {
			executor.execute(task);
			return true;
		} catch (RejectedExecutionException e) {
			LOGGER.warn("Capture executor rejected work (busy or closing): {}", e.toString());
			return false;
		}
	}

	private void writeFile(Path file, Instant startedAt, Instant stoppedAt, StopReason reason,
			List<EntryWire> entries) {
		try {
			json.save(file, new FileWire(
					"zea-capture",
					CURRENT_VERSION,
					startedAt.toEpochMilli(),
					stoppedAt == null ? null : stoppedAt.toEpochMilli(),
					reason == null ? null : reason.name(),
					entries));
		} catch (IOException e) {
			LOGGER.warn("Could not save chat capture {}", file, e);
		}
	}

	/**
	 * One capture file per start, named by the wall clock. A second start
	 * in the same second gets a numeric suffix; across client restarts a
	 * same-second collision would overwrite, which at worst replaces a
	 * seconds-old diagnostic file with zero entries.
	 */
	private Path nextFile() {
		String stamp = FILE_STAMP.format(ZonedDateTime.now());
		if (stamp.equals(lastStamp)) {
			stampCounter++;
		} else {
			lastStamp = stamp;
			stampCounter = 0;
		}
		String suffix = stampCounter == 0 ? "" : "-" + (stampCounter + 1);
		return directory.resolve("capture-" + stamp + suffix + ".json");
	}

	private static final class Session {
		final Path file;
		final Instant startedAt;
		final List<EntryWire> entries = new ArrayList<>();

		Session(Path file, Instant startedAt) {
			this.file = file;
			this.startedAt = startedAt;
		}
	}

	private record EntryWire(long t, String category, String text, String legacy, JsonElement component,
			String error) {
	}

	private record FileWire(String format, int version, long startedAt, Long stoppedAt, String stoppedReason,
			List<EntryWire> entries) {
	}
}
