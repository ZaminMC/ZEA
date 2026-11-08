package net.zaminmc.zea.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStoreTest {
	@TempDir
	Path tempDir;

	private record Button(String command, int x, int y) {
	}

	private final JsonStore store = new JsonStore();

	@Test
	void roundTripsRecords() throws IOException {
		Path file = tempDir.resolve("buttons.json");
		Button[] buttons = {new Button("/spawn", 4, 5), new Button("/town spawn", 6, 7)};

		store.save(file, buttons);
		Button[] loaded = store.load(file, Button[].class, () -> new Button[0]);

		assertArrayEquals(buttons, loaded);
	}

	@Test
	void missingFileYieldsFallback() {
		Path file = tempDir.resolve("missing.json");

		Button[] loaded = store.load(file, Button[].class, () -> new Button[]{new Button("/x", 0, 0)});

		assertEquals(1, loaded.length);
	}

	@Test
	void corruptFileYieldsFallbackAndIsPreserved() throws IOException {
		Path file = tempDir.resolve("buttons.json");
		Files.writeString(file, "{ not json", UTF_8);

		Button[] loaded = store.load(file, Button[].class, () -> new Button[0]);

		assertEquals(0, loaded.length);
		Path preserved = tempDir.resolve("buttons.json.corrupted");
		assertTrue(Files.exists(preserved), "corrupt file should be preserved");
		assertEquals("{ not json", Files.readString(preserved, UTF_8));
	}

	@Test
	void emptyFileYieldsFallback() throws IOException {
		Path file = tempDir.resolve("buttons.json");
		Files.writeString(file, "", UTF_8);

		Button[] loaded = store.load(file, Button[].class, () -> new Button[0]);

		assertEquals(0, loaded.length);
	}
}
