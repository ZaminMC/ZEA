package net.zaminmc.zea.buttons;

/**
 * Pure layout math for inventory buttons. All positions are in GUI units
 * and screen coordinates, computed from the container rectangle the
 * screen reports ({@code leftPos}, {@code topPos}, {@code imageWidth},
 * {@code imageHeight}).
 *
 * <p>An anchor's <em>flush</em> position is where a button's top-left
 * corner sits at offset {@code (0, 0)}: inside the container, aligned
 * with that corner. Positive offsets always move right and down. New
 * buttons default to an offset that places them beside the container
 * instead of over it.
 */
public final class ButtonGeometry {
	/** Button side length in GUI units; icons render at 16 with 1 padding. */
	public static final int SIZE = 18;

	private ButtonGeometry() {
	}

	/** The left edge of a button of {@code anchor} for a container spanning x = [left, left + width). */
	public static int x(ButtonAnchor anchor, int offsetX, int left, int width) {
		return switch (anchor) {
			case TOP_LEFT, BOTTOM_LEFT -> left + offsetX;
			case TOP_RIGHT, BOTTOM_RIGHT -> left + width - SIZE + offsetX;
		};
	}

	/** The top edge of a button of {@code anchor} for a container spanning y = [top, top + height). */
	public static int y(ButtonAnchor anchor, int offsetY, int top, int height) {
		return switch (anchor) {
			case TOP_LEFT, TOP_RIGHT -> top + offsetY;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> top + height - SIZE + offsetY;
		};
	}

	/** The button's left edge. */
	public static int x(InventoryButton button, int left, int width) {
		return x(button.anchor(), button.offsetX(), left, width);
	}

	/** The button's top edge. */
	public static int y(InventoryButton button, int top, int height) {
		return y(button.anchor(), button.offsetY(), top, height);
	}

	/** Whether the point ({@code mx}, {@code my}) lies in the rectangle [x, x+w) × [y, y+h). */
	public static boolean contains(int x, int y, int w, int h, double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Whether the point lies on the button, using the button's own rectangle. */
	public static boolean contains(InventoryButton button, int left, int top, int width, int height, double mx, double my) {
		return contains(x(button, left, width), y(button, top, height), SIZE, SIZE, mx, my);
	}

	/** The x offset that places a button of {@code anchor} with its left edge at {@code bx}. */
	public static int offsetXFor(ButtonAnchor anchor, int left, int width, int bx) {
		return bx - x(anchor, 0, left, width);
	}

	/** The y offset that places a button of {@code anchor} with its top edge at {@code by}. */
	public static int offsetYFor(ButtonAnchor anchor, int top, int height, int by) {
		return by - y(anchor, 0, top, height);
	}

	/** The anchor whose flush position is nearest to ({@code bx}, {@code by}). */
	public static ButtonAnchor nearestAnchor(int left, int top, int width, int height, int bx, int by) {
		ButtonAnchor nearest = ButtonAnchor.TOP_LEFT;
		long best = Long.MAX_VALUE;
		for (ButtonAnchor anchor : ButtonAnchor.values()) {
			int dx = bx - x(anchor, 0, left, width);
			int dy = by - y(anchor, 0, top, height);
			long distance = (long) dx * dx + (long) dy * dy;
			if (distance < best) {
				best = distance;
				nearest = anchor;
			}
		}
		return nearest;
	}
}
