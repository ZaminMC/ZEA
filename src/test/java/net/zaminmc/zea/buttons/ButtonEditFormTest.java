package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButtonEditFormTest {

	private ButtonEditForm form(InventoryButton button) {
		return new ButtonEditForm(button);
	}

	@Test
	void savesValidValues() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		var result = form.save("Home", "town spawn", "minecraft:compass");

		var saved = assertInstanceOf(ButtonEditForm.Result.Saved.class, result);
		assertEquals("Home", saved.button().name());
		assertEquals("town spawn", saved.button().command());
		assertEquals("minecraft:compass", saved.button().icon());
		assertEquals(saved.button(), form.button(), "a successful save updates the working copy");
	}

	@Test
	void refusesMissingCommand() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		var result = form.save("Home", " ", "minecraft:compass");

		var refused = assertInstanceOf(ButtonEditForm.Result.Refused.class, result);
		assertEquals(ButtonEditForm.Rejection.BAD_COMMAND, refused.reason());
	}

	@Test
	void refusesMalformedIcon() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		var result = form.save("Home", "town spawn", "not an item id");

		var refused = assertInstanceOf(ButtonEditForm.Result.Refused.class, result);
		assertEquals(ButtonEditForm.Rejection.BAD_ICON, refused.reason());
	}

	@Test
	void commandErrorWinsWhenBothAreInvalid() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		var result = form.save("Home", "", "not an item id");

		var refused = assertInstanceOf(ButtonEditForm.Result.Refused.class, result);
		assertEquals(ButtonEditForm.Rejection.BAD_COMMAND, refused.reason());
	}

	@Test
	void refusedSaveLeavesWorkingCopyUntouched() {
		InventoryButton button = InventoryButton.newButton(1).withCommand("original");
		ButtonEditForm form = form(button);

		form.save("Home", " ", "minecraft:compass");

		assertEquals("original", form.button().command(),
				"the text fields stay the source of truth until a save succeeds");
	}

	@Test
	void leadingSlashIsAcceptedAndStripped() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		var result = form.save("Home", "/town spawn", "minecraft:compass");

		var saved = assertInstanceOf(ButtonEditForm.Result.Saved.class, result);
		assertEquals("town spawn", saved.button().command());
	}

	@Test
	void anchorAndEnabledMutateTheWorkingCopy() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		form.anchor(ButtonAnchor.BOTTOM_LEFT);
		boolean enabled = form.toggleEnabled();

		assertFalse(enabled);
		assertEquals(ButtonAnchor.BOTTOM_LEFT, form.button().anchor());
		assertFalse(form.button().enabled());
		assertTrue(form.toggleEnabled());
	}

	@Test
	void deleteRequiresTwoClicks() {
		ButtonEditForm form = form(InventoryButton.newButton(1));

		assertFalse(form.deleteArmed());
		assertFalse(form.requestDelete(), "the first click only arms");
		assertTrue(form.deleteArmed());
		assertTrue(form.requestDelete(), "the second click deletes");
	}

	@Test
	void rejectionKeysMatchTheLangFileConvention() {
		assertEquals("command", ButtonEditForm.Rejection.BAD_COMMAND.key());
		assertEquals("icon", ButtonEditForm.Rejection.BAD_ICON.key());
	}

	@Test
	void formKeepsButtonIdentityAcrossEdits() {
		InventoryButton button = InventoryButton.newButton(1);
		ButtonEditForm form = form(button);

		form.anchor(ButtonAnchor.BOTTOM_LEFT);
		form.toggleEnabled();
		var result = form.save("Renamed", "home", "minecraft:compass");

		var saved = assertInstanceOf(ButtonEditForm.Result.Saved.class, result);
		assertEquals(button.id(), saved.button().id());
		assertNotEquals(button, saved.button());
	}
}
