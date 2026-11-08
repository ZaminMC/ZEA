package net.zaminmc.zea.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/**
 * JSON persistence with the safe-write protocol: serialize in memory,
 * verify the serialized form round-trips, then atomically replace the
 * target file. On load, corrupt files are renamed to {@code *.corrupted}
 * (preserved for inspection) and the fallback value is used — a corrupted
 * store never crashes the mod and never produces partially applied state.
 */
public final class JsonStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonStore.class);

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Loads a value, falling back when the file is missing, empty or
	 * corrupt. {@code type} must be a simple Gson-mappable type.
	 */
	public <T> T load(Path file, Class<T> type, Supplier<T> fallback) {
		if (!Files.exists(file)) {
			return fallback.get();
		}
		T value;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			value = gson.fromJson(reader, type);
		} catch (IOException | JsonParseException e) {
			return recoverFromCorruption(file, fallback, e);
		}
		if (value == null) {
			return recoverFromCorruption(file, fallback, null);
		}
		return value;
	}

	/**
	 * Saves a value after verifying its serialized form re-parses. Note:
	 * the round-trip check uses the runtime class, so generic type
	 * information is not verified — store simple types.
	 */
	public void save(Path file, Object value) throws IOException {
		String json = gson.toJson(value);
		if (gson.fromJson(json, value.getClass()) == null) {
			throw new IOException("serialized form of " + file + " failed to round-trip");
		}
		SafeFileWriter.writeAtomically(file, json.getBytes(StandardCharsets.UTF_8));
	}

	private <T> T recoverFromCorruption(Path file, Supplier<T> fallback, Exception cause) {
		if (cause != null) {
			LOGGER.warn("Store {} is corrupt ({}); falling back to defaults", file, cause.toString());
		} else {
			LOGGER.warn("Store {} is empty; falling back to defaults", file);
		}
		try {
			Files.move(file, corrupted(file), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.warn("Could not preserve corrupt store {}", file, e);
		}
		return fallback.get();
	}

	private static Path corrupted(Path file) {
		return file.resolveSibling(file.getFileName().toString() + ".corrupted");
	}
}
