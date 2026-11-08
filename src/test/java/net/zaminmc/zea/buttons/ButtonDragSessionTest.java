package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButtonDragSessionTest {
	private static final int LEFT = 50;
	private static final int TOP = 30;
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;

	private final ButtonDragSession drag = new ButtonDragSession();

	private static InventoryButton topLeft(int offsetX, int offsetY) {
		return InventoryButton.newButton(1).withCommand("g hi")
				.withAnchor(ButtonAnchor.TOP_LEFT).withOffset(offsetX, offsetY);
	}

	@Test
	void idleSessionHasNothingToSay() {
		assertFalse(drag.isActive());
		assertNull(drag.end(LEFT, TOP, WIDTH, HEIGHT));
		drag.cancel();
		assertFalse(drag.isActive());
	}

	@Test
	void pressWithoutMovementKeepsTheButton() {
		InventoryButton button = topLeft(4, 6);
		drag.begin(button, LEFT + 4, TOP + 6, LEFT + 10, TOP + 12);
		drag.moveTo(LEFT + 11, TOP + 13);

		assertTrue(drag.isActive());
		assertFalse(drag.moved(), "two pixels of wiggle are not a drag");
		assertNull(drag.end(LEFT, TOP, WIDTH, HEIGHT), "an unmoved press must not reposition");
		assertFalse(drag.isActive());
	}

	@Test
	void dragMovesThePreviewWithTheCursor() {
		InventoryButton button = topLeft(0, 0);
		drag.begin(button, LEFT, TOP, LEFT + 5, TOP + 5);

		drag.moveTo(LEFT + 40, TOP + 60);

		assertEquals(LEFT + 35, drag.previewX(), "the cursor keeps its place inside the button");
		assertEquals(TOP + 55, drag.previewY());
		assertTrue(drag.moved());
	}

	@Test
	void releasedDragReanchorsToTheNearestCorner() {
		InventoryButton button = topLeft(0, 0);
		drag.begin(button, LEFT, TOP, LEFT + 2, TOP + 2);
		drag.moveTo(LEFT + 40, TOP + 60);

		InventoryButton result = drag.end(LEFT, TOP, WIDTH, HEIGHT);

		assertEquals(button.id(), result.id());
		assertEquals(button.command(), result.command());
		assertEquals(ButtonAnchor.TOP_LEFT, result.anchor());
		assertEquals(38, result.offsetX());
		assertEquals(58, result.offsetY());
	}

	@Test
	void draggedTowardTheRightEdgeAnchorsRight() {
		InventoryButton button = topLeft(0, 0);
		drag.begin(button, LEFT, TOP, LEFT + 2, TOP + 2);
		drag.moveTo(LEFT + WIDTH + 30, TOP + 4);

		InventoryButton result = drag.end(LEFT, TOP, WIDTH, HEIGHT);

		assertEquals(ButtonAnchor.TOP_RIGHT, result.anchor());
		// The flush right position minus the size, expressed as the new offset.
		int expectedX = ButtonGeometry.x(result, LEFT, WIDTH);
		assertEquals(LEFT + WIDTH + 30 - 2, expectedX, "button keeps its left edge under the cursor");
	}

	@Test
	void offsetsAreClampedWhenDraggedFarAway() {
		InventoryButton button = topLeft(0, 0);
		drag.begin(button, LEFT, TOP, LEFT + 2, TOP + 2);
		drag.moveTo(LEFT + 20_000, TOP + 2);

		InventoryButton result = drag.end(LEFT, TOP, WIDTH, HEIGHT);

		assertEquals(InventoryButton.MAX_OFFSET, result.offsetX());
	}

	@Test
	void cancelAbortsWithoutResult() {
		InventoryButton button = topLeft(0, 0);
		drag.begin(button, LEFT, TOP, LEFT + 2, TOP + 2);
		drag.moveTo(LEFT + 100, TOP + 100);

		drag.cancel();

		assertFalse(drag.isActive());
		assertNull(drag.end(LEFT, TOP, WIDTH, HEIGHT));
	}

	@Test
	void beginResetsStateFromAnyEarlierDrag() {
		InventoryButton first = topLeft(0, 0);
		InventoryButton second = topLeft(100, 100);
		drag.begin(first, LEFT, TOP, LEFT + 2, TOP + 2);
		drag.moveTo(LEFT + 100, TOP + 100);
		drag.end(LEFT, TOP, WIDTH, HEIGHT);

		drag.begin(second, LEFT + 100, TOP + 100, LEFT + 105, TOP + 105);

		assertTrue(drag.isActive());
		assertFalse(drag.moved(), "a new grab starts clean");
		assertEquals(LEFT + 100, drag.previewX());
	}
}
