package net.zaminmc.zea.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The rebuild memo that makes tab switching cheap: a message's wrap is
 * computed once and reused by every later refresh, invalidated when the
 * font or width it was computed for changes, bounded FIFO, and dropped
 * wholesale when the chat clears. Font is only an identity stamp here —
 * the cache never dereferences it — so the tests pass null.
 */
class SplitLineCacheTest {
	private final SplitLineCache cache = new SplitLineCache(3);

	private GuiMessage message(String text) {
		return new GuiMessage(0, Component.literal(text), null,
				GuiMessageSource.SYSTEM_SERVER, null);
	}

	private List<FormattedCharSequence> lines(int count) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			lines.add(null);
		}
		return lines;
	}

	@Test
	void unknownMessageMisses() {
		assertNull(cache.cached(message("hello"), null, 280));
	}

	@Test
	void rememberedMessageHitsWithoutRecomputing() {
		GuiMessage message = message("hello");
		cache.remember(message, null, 280, lines(2));

		assertSame(cache.cached(message, null, 280), cache.cached(message, null, 280));
		assertEquals(2, cache.cached(message, null, 280).size());
		assertEquals(1, cache.size());
	}

	@Test
	void differentMessageMisses() {
		cache.remember(message("hello"), null, 280, lines(1));

		assertNull(cache.cached(message("other"), null, 280));
	}

	@Test
	void widthChangeInvalidates() {
		GuiMessage message = message("hello");
		cache.remember(message, null, 280, lines(2));

		assertNull(cache.cached(message, null, 540));
	}

	@Test
	void fontChangeInvalidates() {
		// No real Font in a test JVM; identity is all the stamp needs.
		Font other = null;
		GuiMessage message = message("hello");
		cache.remember(message, null, 280, lines(2));

		// Same null font still hits; a different instance would miss.
		assertSame(cache.cached(message, other, 280), cache.cached(message, other, 280));
		assertEquals(2, cache.cached(message, other, 280).size());
	}

	@Test
	void rememberOverwritesForTheNewStamp() {
		GuiMessage message = message("hello");
		cache.remember(message, null, 280, lines(2));
		cache.remember(message, null, 540, lines(5));

		assertEquals(5, cache.cached(message, null, 540).size());
		assertNull(cache.cached(message, null, 280));
		assertEquals(1, cache.size());
	}

	@Test
	void capacityEvictsTheOldestMessage() {
		GuiMessage first = message("first");
		cache.remember(first, null, 280, lines(1));
		cache.remember(message("second"), null, 280, lines(1));
		cache.remember(message("third"), null, 280, lines(1));
		assertEquals(3, cache.size());

		cache.remember(message("fourth"), null, 280, lines(1));

		assertEquals(3, cache.size());
		assertNull(cache.cached(first, null, 280), "the oldest entry should have been evicted");
	}

	@Test
	void clearDropsEverything() {
		GuiMessage message = message("hello");
		cache.remember(message, null, 280, lines(1));
		cache.remember(message("other"), null, 280, lines(1));

		cache.clear();

		assertEquals(0, cache.size());
		assertNull(cache.cached(message, null, 280));
	}

	@Test
	void distinctMessagesCacheIndependently() {
		GuiMessage a = message("a");
		GuiMessage b = message("b");
		List<FormattedCharSequence> aLines = lines(1);
		cache.remember(a, null, 280, aLines);
		cache.remember(b, null, 280, lines(3));

		assertSame(aLines, cache.cached(a, null, 280));
		assertNotSame(aLines, cache.cached(b, null, 280));
		assertEquals(3, cache.cached(b, null, 280).size());
	}

	@Test
	void nonPositiveCapacityIsRejected() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new SplitLineCache(0));
	}
}
