package net.zaminmc.zea.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeFileWriterTest {
	@TempDir
	Path tempDir;

	@Test
	void writesReadableContent() throws IOException {
		Path target = tempDir.resolve("store.json");

		SafeFileWriter.writeAtomically(target, "{\"a\":1}".getBytes(UTF_8));

		assertEquals("{\"a\":1}", Files.readString(target, UTF_8));
	}

	@Test
	void overwritesExistingContent() throws IOException {
		Path target = tempDir.resolve("store.json");
		Files.writeString(target, "old");

		SafeFileWriter.writeAtomically(target, "new".getBytes(UTF_8));

		assertEquals("new", Files.readString(target, UTF_8));
	}

	@Test
	void createsMissingParentDirectories() throws IOException {
		Path target = tempDir.resolve("a/b/c/store.json");

		SafeFileWriter.writeAtomically(target, "x".getBytes(UTF_8));

		assertEquals("x", Files.readString(target, UTF_8));
	}

	@Test
	void leavesNoTempFilesBehind() throws IOException {
		Path target = tempDir.resolve("store.json");

		SafeFileWriter.writeAtomically(target, "x".getBytes(UTF_8));
		SafeFileWriter.writeAtomically(target, "y".getBytes(UTF_8));

		try (Stream<Path> files = Files.list(tempDir)) {
			List<String> names = files.map(p -> p.getFileName().toString()).toList();
			assertEquals(1, names.size());
			assertTrue(names.contains("store.json"));
		}
	}
}
