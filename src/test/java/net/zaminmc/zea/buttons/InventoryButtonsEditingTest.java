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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryButtonsEditingTest {
	@TempDir
	Path tempDir;

	private final List<String> sent = new ArrayList<>();

	private InventoryButtons withButtons(String json) throws IOException {
		Path file = tempDir.resolve("buttons.json");
		Files.writeString(file, json, UTF_8);
		return new InventoryButtons(file, new ButtonActivator(sent::add, 0));
	}

	private static String oneButton(String name) {
		return """
				{
				  "version": 1,
				  "buttons": [
				    { "name": "%s", "command": "town spawn", "icon": "minecraft:compass" }
				  ]
				}
				""".formatted(name);
	}

	@Test
	void upsertReplacesByIdAndPersists() throws IOException {
		InventoryButtons buttons = withButtons(oneButton("Home"));
		InventoryButton original = buttons.buttons().get(0);

		buttons.upsert(original.withName("Renamed").withCommand("home"));

		assertEquals(1, buttons.buttons().size());
		assertEquals("Renamed", buttons.buttons().get(0).name());
		assertEquals("home", buttons.buttons().get(0).command());
		assertEquals(List.of(buttons.buttons().get(0)), new InventoryButtons(
				tempDir.resolve("buttons.json"), new ButtonActivator(sent::add, 0)).buttons(),
				"the change must survive a reload");
	}

	@Test
	void upsertAppendsUnknownIds() throws IOException {
		InventoryButtons buttons = withButtons(oneButton("Home"));

		buttons.upsert(InventoryButton.newButton(2).withCommand("vote"));

		assertEquals(2, buttons.buttons().size());
		assertEquals("Button 2", buttons.buttons().get(1).name());
	}

	@Test
	void deleteRemovesByIdAndPersists() throws IOException {
		InventoryButtons buttons = withButtons(oneButton("Home"));
		InventoryButton home = buttons.buttons().get(0);

		buttons.delete(home.id());

		assertEquals(List.of(), buttons.buttons());
		assertEquals(List.of(), new InventoryButtons(
				tempDir.resolve("buttons.json"), new ButtonActivator(sent::add, 0)).buttons());
	}

	@Test
	void editModeTogglesAndDropsTheDrag() throws IOException {
		InventoryButtons buttons = withButtons(oneButton("Home"));
		assertFalse(buttons.editMode());

		buttons.toggleEditMode();
		assertTrue(buttons.editMode());

		InventoryButton home = buttons.buttons().get(0);
		buttons.drag().begin(home, 0, 0, 5, 5);
		buttons.toggleEditMode();

		assertFalse(buttons.editMode());
		assertFalse(buttons.drag().isActive(), "leaving edit mode aborts the drag");
	}

	@Test
	void editableButtonAtIncludesDisabledButtons() throws IOException {
		InventoryButtons buttons = withButtons("""
				{
				  "version": 1,
				  "buttons": [
				    { "name": "Hidden", "command": "town spawn", "icon": "minecraft:compass", "anchor": "TOP_LEFT", "offsetX": 60, "offsetY": 60, "enabled": false }
				  ]
				}
				""");
		InventoryButton hidden = buttons.buttons().get(0);
		int x = ButtonGeometry.x(hidden, 40, 176);
		int y = ButtonGeometry.y(hidden, 20, 166);

		assertNull(buttons.buttonAt(40, 20, 176, 166, x + 1, y + 1));
		assertEquals(hidden, buttons.editableButtonAt(40, 20, 176, 166, x + 1, y + 1));
	}
}
