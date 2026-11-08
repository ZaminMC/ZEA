package net.zaminmc.zea.persistence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Atomic file replacement: content is staged in a sibling temp file and
 * moved over the target in one step, so readers never observe a torn or
 * partially written file (the pattern proven by NEU's ConfigUtil).
 *
 * <p>Callers that need write-then-verify semantics (JSON round-trip
 * checks) use {@link JsonStore}; this class only guarantees the atomic
 * replacement itself.
 */
public final class SafeFileWriter {
	private SafeFileWriter() {
	}

	public static void writeAtomically(Path target, byte[] content) throws IOException {
		Path absolute = target.toAbsolutePath();
		Path parent = absolute.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Path temp = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
		try {
			Files.write(temp, content);
			try {
				Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
