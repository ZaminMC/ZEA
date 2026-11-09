package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryButtonsTest {
	@TempDir
	Path tempDir;

	private final List<String> sent = new ArrayList<>();

	private ButtonActivator activator() {
		return new ButtonActivator(sent::add, 0);
	}

	private static final int LEFT = 40;
	private static final int TOP = 20;
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;

	private InventoryButtons loadedWith(String json) throws IOException {
		Path file = tempDir.resolve("buttons.json");
		Files.writeString(file, json, UTF_8);
		return new InventoryButtons(file, activator());
	}

	@Test
	void loadsButtonsFromTheStore() throws IOException {
		InventoryButtons buttons = loadedWith("""
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "name": "Home", "command": "town spawn", "icon": "minecraft:compass" }
                                  ]
                                }
                                """);

		assertEquals(1, buttons.buttons().size());
		assertEquals("Home", buttons.buttons().get(0).name());
	}

	@Test
	void missingFileYieldsNoButtons() {
		InventoryButtons buttons = new InventoryButtons(tempDir.resolve("absent.json"), activator());

		assertEquals(List.of(), buttons.buttons());
	}

	@Test
	void buttonAtFindsEnabledButtonsOnly() throws IOException {
		InventoryButtons buttons = loadedWith("""
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "name": "Home", "command": "town spawn", "icon": "minecraft:compass" },
                                    { "name": "Hidden", "command": "town spawn", "icon": "minecraft:compass", "anchor": "TOP_LEFT", "offsetX": 60, "offsetY": 60, "enabled": false }
                                  ]
                                }
                                """);
		InventoryButton home = buttons.buttons().get(0);
		InventoryButton hidden = buttons.buttons().get(1);
		int homeX = ButtonGeometry.x(home, LEFT, WIDTH);
		int homeY = ButtonGeometry.y(home, TOP, HEIGHT);
		int hiddenX = ButtonGeometry.x(hidden, LEFT, WIDTH);
		int hiddenY = ButtonGeometry.y(hidden, TOP, HEIGHT);

		assertEquals(home, buttons.buttonAt(LEFT, TOP, WIDTH, HEIGHT, homeX + 1, homeY + 1));
		assertNull(buttons.buttonAt(LEFT, TOP, WIDTH, HEIGHT, hiddenX + 1, hiddenY + 1),
				"disabled buttons are not hit");
		assertNull(buttons.buttonAt(LEFT, TOP, WIDTH, HEIGHT, 0, 0));
	}

	@Test
	void overlappingButtonsResolveToTheLastDrawn() throws IOException {
		InventoryButtons buttons = loadedWith("""
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "name": "Under", "command": "spawn", "icon": "minecraft:compass", "anchor": "TOP_LEFT", "offsetX": 0, "offsetY": 0 },
                                    { "name": "Over", "command": "spawn", "icon": "minecraft:paper", "anchor": "TOP_LEFT", "offsetX": 4, "offsetY": 4 }
                                  ]
                                }
                                """);

		InventoryButton hit = buttons.buttonAt(LEFT, TOP, WIDTH, HEIGHT, LEFT + 5, TOP + 5);

		assertEquals("Over", hit.name(), "the button drawn on top must win the hit test");
	}

	@Test
	void clickRunsTheButtonThroughTheActivator() throws IOException {
		InventoryButtons buttons = loadedWith("""
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "name": "Home", "command": "town spawn", "icon": "minecraft:compass" }
                                  ]
                                }
                                """);
		InventoryButton home = buttons.buttons().get(0);

		assertEquals(ButtonActivator.Result.EXECUTED, buttons.click(home, 1_000));

		assertEquals(List.of("town spawn"), sent);
	}
}
