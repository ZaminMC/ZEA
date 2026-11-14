package net.zaminmc.zea.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ModMenu wiring: ZEA declares the {@code modmenu} entrypoint so
 * ModMenu (when the player has it) shows a config button on ZEA's entry
 * that opens the same MoulConfig editor {@code /zea} does. The entrypoint
 * class must implement ModMenu's API and must stay loadable only when
 * ModMenu is present — ZEA itself carries no runtime dependency.
 */
class ModMenuWiringTest {
	private static final Path FABRIC_MOD_JSON = Paths.get(
			"src", "main", "resources", "fabric.mod.json");

	@Test
	void theModMenuEntrypointIsDeclaredAndPointsAtTheIntegration() throws IOException {
		JsonObject root = JsonParser.parseString(Files.readString(FABRIC_MOD_JSON)).getAsJsonObject();

		assertTrue(root.has("entrypoints"), "fabric.mod.json declares no entrypoints");
		JsonObject entrypoints = root.getAsJsonObject("entrypoints");
		assertTrue(entrypoints.has("modmenu"),
				"the modmenu entrypoint is missing — ModMenu cannot find the config button");

		String entrypoint = entrypoints.getAsJsonArray("modmenu").get(0).getAsString();
		assertEquals("net.zaminmc.zea.config.ModMenuIntegration", entrypoint);
	}

	@Test
	void theIntegrationImplementsModMenuApi() {
		assertTrue(ModMenuApi.class.isAssignableFrom(ModMenuIntegration.class),
				"ModMenuIntegration must implement ModMenuApi to be a valid entrypoint");
	}

	@Test
	void modMenuStaysAnOptionalRuntimeDependency() throws IOException {
		JsonObject root = JsonParser.parseString(Files.readString(FABRIC_MOD_JSON)).getAsJsonObject();

		assertTrue(!root.has("depends") || !root.getAsJsonObject("depends").has("modmenu"),
				"modmenu must not be a hard dependency — ZEA works without it");
		assertTrue(!root.has("jars"),
				"no mod jar may be bundled for ModMenu — players install it themselves");
	}
}
