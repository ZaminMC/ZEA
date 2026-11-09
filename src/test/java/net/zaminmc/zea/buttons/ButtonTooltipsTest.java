package net.zaminmc.zea.buttons;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ButtonTooltipsTest {
	private static InventoryButton button() {
		return InventoryButton.newButton(1).withName("Home").withCommand("town spawn")
				.withIcon("minecraft:compass");
	}

	@Test
	void showsNameAndCommand() {
		List<Component> lines = ButtonTooltips.lines(button(), true);

		assertEquals(2, lines.size());
		assertEquals("Home", lines.get(0).getString());
		assertEquals("/town spawn", lines.get(1).getString());
	}

	@Test
	void warnsAboutUnknownIcons() {
		List<Component> lines = ButtonTooltips.lines(button(), false);

		assertEquals(3, lines.size());
		TranslatableContents warning = assertInstanceOf(TranslatableContents.class, lines.get(2).getContents());
		assertEquals("zea.buttons.unknown_icon", warning.getKey());
		assertEquals(1, warning.getArgs().length);
		assertEquals("minecraft:compass", warning.getArgs()[0]);
	}
}
