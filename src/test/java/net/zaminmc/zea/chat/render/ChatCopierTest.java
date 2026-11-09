package net.zaminmc.zea.chat.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the copy text-selection policy: plain copies flatten styles away,
 * formatted copies preserve them as legacy codes.
 */
class ChatCopierTest {
	private static GuiMessage message(Component content) {
		return new GuiMessage(0, content, null, GuiMessageSource.PLAYER, null);
	}

	@Test
	void plainCopyFlattensStylesAway() {
		MutableComponent name = Component.literal("Aurora").withStyle(ChatFormatting.AQUA);
		MutableComponent content = Component.literal(" hello world").withStyle(ChatFormatting.WHITE);
		String copied = ChatCopier.textFor(message(name.append(content)), false);
		assertEquals("Aurora hello world", copied);
	}

	@Test
	void formattedCopyKeepsLegacyCodes() {
		MutableComponent name = Component.literal("Aurora").withStyle(ChatFormatting.AQUA);
		MutableComponent content = Component.literal(" hello world").withStyle(ChatFormatting.WHITE);
		String copied = ChatCopier.textFor(message(name.append(content)), true);
		// The first run needs no reset; each later style change emits §r + its codes.
		assertEquals("§bAurora§r§f hello world", copied);
	}

	@Test
	void unstyledMessageCopiesIdenticallyInBothModes() {
		String copied = ChatCopier.textFor(message(Component.literal("Server restarting soon")), true);
		assertEquals(ChatCopier.textFor(message(Component.literal("Server restarting soon")), false), copied);
		assertEquals("Server restarting soon", copied);
	}

	@Test
	void earthMcSegmentStyleRoundTrips() {
		// The observed EarthMC shape: gray prefix run, then white content run.
		MutableComponent prefix = Component.literal("[N] ").withStyle(ChatFormatting.GRAY);
		MutableComponent name = Component.literal("Aurora").withStyle(ChatFormatting.AQUA);
		MutableComponent content = Component.literal(": hi").withStyle(ChatFormatting.WHITE);
		String copied = ChatCopier.textFor(message(prefix.append(name).append(content)), true);
		assertTrue(copied.contains("§7[N] "), "prefix keeps its gray code: " + copied);
		assertTrue(copied.contains("§bAurora"), "name keeps its aqua code: " + copied);
		assertTrue(copied.endsWith("§r§f: hi"), "content resets before its white code: " + copied);
	}
}
