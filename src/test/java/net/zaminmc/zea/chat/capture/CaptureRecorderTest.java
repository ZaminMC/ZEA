package net.zaminmc.zea.chat.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureRecorderTest {
	@TempDir
	Path dir;

	private final List<CaptureRecorder.Stopped> autoStops = new ArrayList<>();

	/** The serializer the recorder sees; canned JSON unless a test says otherwise. */
	private CaptureRecorder.Serializer serializer = component ->
			new ComponentJson.Result(new JsonPrimitive(component.getString()), null);

	private CaptureRecorder recorder() {
		return new CaptureRecorder(dir.resolve("capture"), serializer, Runnable::run, Runnable::run,
				autoStops::add);
	}

	private static ClassifiedMessage message(String text) {
		return ClassifiedMessage.unclassified(CapturedMessage.of(Component.literal(text)));
	}

	private static JsonObject read(Path file) throws IOException {
		return JsonParser.parseReader(Files.newBufferedReader(file)).getAsJsonObject();
	}

	@Test
	void inactiveUntilStarted() {
		CaptureRecorder recorder = recorder();

		assertFalse(recorder.status().active());
		assertNull(recorder.stop());
		recorder.onClassified(message("hi"));
		assertEquals(0, recorder.status().recorded());
	}

	@Test
	void recordsEntriesToAStampedFile() throws IOException {
		CaptureRecorder recorder = recorder();
		Path file = recorder.start();

		assertNotNull(file);
		assertTrue(file.getFileName().toString().startsWith("capture-"), file.toString());
		recorder.onClassified(message("first"));
		recorder.onClassified(message("second"));
		assertTrue(recorder.status().active());
		assertEquals(2, recorder.status().recorded());

		CaptureRecorder.Stopped stopped = recorder.stop();

		assertNotNull(stopped);
		assertEquals(2, stopped.recorded());
		assertEquals(file, stopped.file());
		assertEquals(CaptureRecorder.StopReason.EXPLICIT, stopped.reason());
		assertFalse(recorder.status().active());

		JsonObject root = read(file);
		assertEquals("zea-capture", root.get("format").getAsString());
		assertEquals(CaptureRecorder.CURRENT_VERSION, root.get("version").getAsInt());
		assertEquals("EXPLICIT", root.get("stoppedReason").getAsString());
		assertEquals(2, root.getAsJsonArray("entries").size());
		JsonObject first = root.getAsJsonArray("entries").get(0).getAsJsonObject();
		assertEquals("first", first.get("text").getAsString());
		assertEquals("UNCLASSIFIED", first.get("category").getAsString());
		assertEquals("first", first.get("legacy").getAsString());
		assertEquals("first", first.get("component").getAsString());
	}

	@Test
	void blankMessagesAreSkipped() throws IOException {
		CaptureRecorder recorder = recorder();
		Path file = recorder.start();

		recorder.onClassified(message("  "));
		recorder.stop();

		assertEquals(0, read(file).getAsJsonArray("entries").size());
	}

	@Test
	void stopsItselfAtTheCapAndReports() throws IOException {
		CaptureRecorder recorder = recorder();
		Path file = recorder.start();

		for (int i = 0; i < CaptureRecorder.MAX_ENTRIES; i++) {
			recorder.onClassified(message("m" + i));
		}

		assertFalse(recorder.status().active());
		assertEquals(1, autoStops.size());
		assertEquals(CaptureRecorder.StopReason.CAP, autoStops.get(0).reason());
		assertEquals(CaptureRecorder.MAX_ENTRIES, autoStops.get(0).recorded());
		assertEquals(CaptureRecorder.MAX_ENTRIES, read(file).getAsJsonArray("entries").size());

		recorder.onClassified(message("late"));
		assertEquals(CaptureRecorder.MAX_ENTRIES, read(file).getAsJsonArray("entries").size(),
				"a stopped session records nothing further");
	}

	@Test
	void serializationFailureKeepsTheEntryWithItsReason() throws IOException {
		serializer = component -> new ComponentJson.Result(null, "no registries");
		CaptureRecorder recorder = recorder();
		Path file = recorder.start();

		recorder.onClassified(message("text only"));
		recorder.stop();

		JsonObject entry = read(file).getAsJsonArray("entries").get(0).getAsJsonObject();
		assertEquals("text only", entry.get("text").getAsString());
		assertEquals("no registries", entry.get("error").getAsString());
		assertTrue(entry.get("component") instanceof JsonNull || entry.get("component") == null,
				"a failed serialization leaves no component behind");
	}

	@Test
	void aSecondStartInTheSameSecondGetsItsOwnFile() {
		CaptureRecorder recorder = recorder();
		Path first = recorder.start();

		assertNull(recorder.start(), "a live session is never disturbed by a second start");
		recorder.stop();

		Path second = recorder.start();
		assertTrue(!first.equals(second), "each start gets its own file");
		recorder.stop();
	}

	@Test
	void intermediateFlushesWriteAtTheIntervalWhileRunning() throws IOException {
		CaptureRecorder recorder = recorder();
		Path file = recorder.start();

		for (int i = 0; i < 30; i++) {
			recorder.onClassified(message("m" + i));
		}

		assertTrue(recorder.status().active());
		JsonObject snapshot = read(file);
		assertEquals(25, snapshot.getAsJsonArray("entries").size(),
				"the file holds the last interval snapshot while the session runs");
		assertNull(snapshot.get("stoppedReason"), "a running capture's snapshot has no stop");

		recorder.flush();
		assertEquals(30, read(file).getAsJsonArray("entries").size());
	}
}
