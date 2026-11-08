package net.zaminmc.zea.chat.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatGeometryTest {
	// Vanilla defaults: gui height 240, chat scale 1, line spacing 0.
	// chatBottom = 240 - 40 = 200; line 0's text occupies [192, 201),
	// line 1's [183, 192), line 2's [174, 183).
	private static final int CHAT_BOTTOM = ChatGeometry.chatBottom(240, 1);
	private static final int ENTRY_HEIGHT = ChatGeometry.entryHeight(0);
	private static final int TO_MESSAGE_Y = ChatGeometry.entryBottomToMessageY(0);

	@Test
	void chatBottomSitsFortyPixelsAboveTheScreenBottom() {
		assertEquals(200, ChatGeometry.chatBottom(240, 1));
		assertEquals(440, ChatGeometry.chatBottom(480, 1));
	}

	@Test
	void chatBottomDividesByScale() {
		// scale 0.5 doubles the unscaled extent
		assertEquals(400, ChatGeometry.chatBottom(240, 0.5));
	}

	@Test
	void entryMetricsUseVanillaFormulas() {
		assertEquals(9, ChatGeometry.entryHeight(0));
		assertEquals(8, ChatGeometry.entryBottomToMessageY(0));
	}

	@Test
	void textRegionsStackFromTheBottom() {
		assertEquals(192, CHAT_BOTTOM - TO_MESSAGE_Y);
		// slot 0: [192, 201), slot 1: [183, 192), slot 2: [174, 183)
		assertEquals(0, slot(192));
		assertEquals(0, slot(200.9));
		assertEquals(1, slot(191.9));
		assertEquals(1, slot(183));
		assertEquals(2, slot(182.9));
		assertEquals(2, slot(174));
	}

	@Test
	void belowTheLowestLineIsOutsideTheTextArea() {
		assertEquals(-1, slot(201));
		assertEquals(-1, slot(220));
	}

	@Test
	void horizontalBoundsExcludeSideMargins() {
		assertEquals(-1, ChatGeometry.lineSlotAt(-1, 195, CHAT_BOTTOM, 320, ENTRY_HEIGHT, TO_MESSAGE_Y));
		assertEquals(-1, ChatGeometry.lineSlotAt(320, 195, CHAT_BOTTOM, 320, ENTRY_HEIGHT, TO_MESSAGE_Y));
		assertEquals(0, ChatGeometry.lineSlotAt(319.9, 195, CHAT_BOTTOM, 320, ENTRY_HEIGHT,
				TO_MESSAGE_Y));
	}

	@Test
	void slotsAboveThePanelAreReportedAndCallersClampThem() {
		// Far above the panel top: the slot exists mathematically; the
		// caller rejects it against the drawn line count.
		assertEquals(21, slot(CHAT_BOTTOM - 21 * ENTRY_HEIGHT - TO_MESSAGE_Y));
	}

	private static int slot(double localY) {
		return ChatGeometry.lineSlotAt(10, localY, CHAT_BOTTOM, 320, ENTRY_HEIGHT, TO_MESSAGE_Y);
	}
}
