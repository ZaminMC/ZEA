package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButtonGeometryTest {
	/** A typical container: the 176 x 166 player inventory. */
	private static final int LEFT = 50;
	private static final int TOP = 30;
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;

	private static InventoryButton at(ButtonAnchor anchor, int offsetX, int offsetY) {
		return InventoryButton.newButton(1).withCommand("g hi").withAnchor(anchor).withOffset(offsetX, offsetY);
	}

	@Test
	void leftAnchorsMeasureFromTheContainerLeft() {
		assertEquals(LEFT + 4, ButtonGeometry.x(at(ButtonAnchor.TOP_LEFT, 4, 0), LEFT, WIDTH));
		assertEquals(LEFT - 20, ButtonGeometry.x(at(ButtonAnchor.BOTTOM_LEFT, -20, 0), LEFT, WIDTH));
	}

	@Test
	void rightAnchorsMeasureFromTheFlushRightCorner() {
		int flush = LEFT + WIDTH - ButtonGeometry.SIZE;
		assertEquals(flush + 6, ButtonGeometry.x(at(ButtonAnchor.TOP_RIGHT, 6, 0), LEFT, WIDTH));
		assertEquals(flush, ButtonGeometry.x(at(ButtonAnchor.BOTTOM_RIGHT, 0, 0), LEFT, WIDTH));
	}

	@Test
	void bottomAnchorsMeasureFromTheFlushBottomCorner() {
		int flush = TOP + HEIGHT - ButtonGeometry.SIZE;
		assertEquals(flush + 8, ButtonGeometry.y(at(ButtonAnchor.BOTTOM_LEFT, 0, 8), TOP, HEIGHT));
		assertEquals(TOP + HEIGHT - ButtonGeometry.SIZE + 3, ButtonGeometry.y(at(ButtonAnchor.BOTTOM_RIGHT, 0, 3), TOP, HEIGHT));
	}

	@Test
	void topAnchorsMeasureFromTheContainerTop() {
		assertEquals(TOP + 2, ButtonGeometry.y(at(ButtonAnchor.TOP_RIGHT, 0, 2), TOP, HEIGHT));
	}

	@Test
	void defaultOffsetPlacesNewButtonsBesideTheContainer() {
		InventoryButton button = InventoryButton.newButton(1).withCommand("g hi");

		int x = ButtonGeometry.x(button, LEFT, WIDTH);
		int y = ButtonGeometry.y(button, TOP, HEIGHT);

		assertEquals(LEFT + WIDTH + 4, x, "button should sit just right of the GUI");
		assertEquals(TOP + 2, y, "button should align with the GUI top");
	}

	@Test
	void containsUsesHalfOpenEdges() {
		int x = ButtonGeometry.x(at(ButtonAnchor.TOP_LEFT, 0, 0), LEFT, WIDTH);
		int y = ButtonGeometry.y(at(ButtonAnchor.TOP_LEFT, 0, 0), TOP, HEIGHT);

		assertTrue(ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE, x, y));
		assertTrue(ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE,
				x + ButtonGeometry.SIZE - 0.5, y + ButtonGeometry.SIZE - 0.5));
		assertFalse(ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE,
				x + ButtonGeometry.SIZE, y));
		assertFalse(ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE, x - 0.1, y));
	}

	@Test
	void offsetsRoundTripThroughPosition() {
		for (ButtonAnchor anchor : ButtonAnchor.values()) {
			int bx = 137;
			int by = 96;
			int offsetX = ButtonGeometry.offsetXFor(anchor, LEFT, WIDTH, bx);
			int offsetY = ButtonGeometry.offsetYFor(anchor, TOP, HEIGHT, by);

			assertEquals(bx, ButtonGeometry.x(anchor, offsetX, LEFT, WIDTH),
					"offset should place the button at bx for " + anchor);
			assertEquals(by, ButtonGeometry.y(anchor, offsetY, TOP, HEIGHT),
					"offset should place the button at by for " + anchor);
		}
	}

	@Test
	void nearestAnchorPicksTheClosestCorner() {
		assertEquals(ButtonAnchor.TOP_LEFT,
				ButtonGeometry.nearestAnchor(LEFT, TOP, WIDTH, HEIGHT, LEFT + 2, TOP + 2));
		assertEquals(ButtonAnchor.TOP_RIGHT,
				ButtonGeometry.nearestAnchor(LEFT, TOP, WIDTH, HEIGHT, LEFT + WIDTH + 30, TOP - 10));
		assertEquals(ButtonAnchor.BOTTOM_LEFT,
				ButtonGeometry.nearestAnchor(LEFT, TOP, WIDTH, HEIGHT, LEFT - 30, TOP + HEIGHT + 5));
		assertEquals(ButtonAnchor.BOTTOM_RIGHT,
				ButtonGeometry.nearestAnchor(LEFT, TOP, WIDTH, HEIGHT, LEFT + WIDTH + 30, TOP + HEIGHT + 5));
	}

	@Test
	void anchoringKeepsButtonsStableAcrossContainerSizes() {
		InventoryButton button = at(ButtonAnchor.TOP_RIGHT, 6, 2);

		int besideInventory = ButtonGeometry.x(button, LEFT, WIDTH);
		int besideChest = ButtonGeometry.x(button, LEFT, WIDTH - 36);

		assertEquals(besideInventory - 36, besideChest,
				"a right-anchored button should follow the smaller right edge");
	}
}
