package net.zaminmc.zea.chat.capture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads capture files written by {@link CaptureRecorder} as
 * component-level fixtures: each entry's component JSON is decoded back
 * into a live component through the same codec that wrote it, so a
 * capture taken in-game drops straight into a regression test.
 */
public final class CaptureFixtures {
	private CaptureFixtures() {
	}

	/** One captured message in fixture form. */
	public record Fixture(String category, String text, Component component) {
	}

	/**
	 * Loads a {@code zea-capture} file. Entries whose component failed to
	 * serialize in-game are skipped — their text survived, but a fixture
	 * without the component is not a component fixture.
	 */
	public static List<Fixture> load(Path file) throws IOException {
		JsonObject root;
		try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
		}
		if (!"zea-capture".equals(string(root, "format"))
					|| root.get("version").getAsInt() != CaptureRecorder.CURRENT_VERSION) {
			throw new IOException("not a zea-capture version "
					+ CaptureRecorder.CURRENT_VERSION + " file: " + file);
		}
		List<Fixture> fixtures = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("entries")) {
			JsonObject entry = element.getAsJsonObject();
			JsonElement component = entry.get("component");
			if (component == null || component.isJsonNull()) {
				continue;
			}
			Component decoded = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, component)
					.result()
					.orElseThrow(() -> new IOException("undecodable component in " + file));
			fixtures.add(new Fixture(string(entry, "category"), string(entry, "text"), decoded));
		}
		return List.copyOf(fixtures);
	}

	private static String string(JsonObject object, String field) {
		JsonElement value = object.get(field);
		return value == null || value.isJsonNull() ? "" : value.getAsString();
	}
}
