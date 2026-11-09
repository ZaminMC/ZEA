package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryButtonTest {
	@Test
	void newButtonIsIncompleteUntilGivenACommand() {
		InventoryButton button = InventoryButton.newButton(1);

		assertEquals("Button 1", button.name());
		assertEquals("", button.command());
		assertEquals("minecraft:compass", button.icon());
		assertEquals(ButtonAnchor.TOP_RIGHT, button.anchor());
		assertEquals(InventoryButton.DEFAULT_OFFSET_X, button.offsetX());
		assertEquals(InventoryButton.DEFAULT_OFFSET_Y, button.offsetY());
		assertTrue(button.enabled());
		assertFalse(button.isUsable());

		assertTrue(button.withCommand("g hello").isUsable());
	}

	@Test
	void missingFieldsGetSafeDefaults() {
		InventoryButton button = new InventoryButton(null, null, null, null, null, 0, 0, true);

		assertNotNull(button.id());
		assertEquals(InventoryButton.DEFAULT_NAME, button.name());
		assertEquals("", button.command());
		assertEquals("", button.icon());
		assertEquals(ButtonAnchor.TOP_RIGHT, button.anchor());
		assertFalse(button.isUsable());
	}

	@Test
	void leadingSlashIsStrippedFromCommands() {
		InventoryButton button = InventoryButton.newButton(1).withCommand("/town spawn");

		assertEquals("town spawn", button.command());
	}

	@Test
	void longNamesAndCommandsAreTruncated() {
		InventoryButton button = InventoryButton.newButton(1)
				.withCommand("a".repeat(InventoryButton.MAX_COMMAND_LENGTH + 10))
				.withName("b".repeat(InventoryButton.MAX_NAME_LENGTH + 10));

		assertEquals(InventoryButton.MAX_COMMAND_LENGTH, button.command().length());
		assertEquals(InventoryButton.MAX_NAME_LENGTH, button.name().length());
	}

	@Test
	void controlCharactersAreRemoved() {
		InventoryButton button = InventoryButton.newButton(1).withCommand("town\r\nspawn");

		assertEquals("townspawn", button.command());
		assertTrue(button.isUsable());
	}

	@Test
	void offsetsAreClamped() {
		InventoryButton button = InventoryButton.newButton(1).withOffset(10_000, -10_000);

		assertEquals(InventoryButton.MAX_OFFSET, button.offsetX());
		assertEquals(InventoryButton.MIN_OFFSET, button.offsetY());
		assertTrue(button.offsetClamped());
	}

	@Test
	void commandValidityRules() {
		assertFalse(InventoryButton.validCommand(null));
		assertFalse(InventoryButton.validCommand(""));
		assertFalse(InventoryButton.validCommand("   "));
		assertFalse(InventoryButton.validCommand("a".repeat(InventoryButton.MAX_COMMAND_LENGTH + 1)));
		assertFalse(InventoryButton.validCommand("town\nspawn"));
		assertTrue(InventoryButton.validCommand("g hello world"));
		assertTrue(InventoryButton.validCommand("a".repeat(InventoryButton.MAX_COMMAND_LENGTH)));
	}

	@Test
	void iconValidityRules() {
		assertFalse(InventoryButton.validIcon(null));
		assertFalse(InventoryButton.validIcon(""));
		assertFalse(InventoryButton.validIcon("a".repeat(InventoryButton.MAX_ICON_LENGTH + 1)));
		assertFalse(InventoryButton.validIcon("compass\n"));
		assertTrue(InventoryButton.validIcon("minecraft:compass"));
		assertTrue(InventoryButton.validIcon("zea:anything"));
	}

	@Test
	void withNewIdChangesIdentityButKeepsData() {
		InventoryButton button = InventoryButton.newButton(1).withCommand("g hi");
		InventoryButton copy = button.withNewId();

		assertNotEquals(button.id(), copy.id());
		assertEquals(button.command(), copy.command());
		assertEquals(button.icon(), copy.icon());
		assertEquals(button.anchor(), copy.anchor());
	}
}
