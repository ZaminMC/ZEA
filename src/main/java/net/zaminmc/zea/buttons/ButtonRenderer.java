package net.zaminmc.zea.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws the buttons of a container screen. Pure presentation: positions
 * come from {@link ButtonGeometry}, icons from {@link ButtonIcons},
 * tooltip text from {@link ButtonTooltips}; this class owns only the
 * rectangles and colors.
 *
 * <p>Drawing is a slot-like box — a one-pixel border over a dark fill —
 * so buttons read as part of the GUI without new textures.
 */
public final class ButtonRenderer {
	/** Border color of an idle button (slate). */
	private static final int BORDER = 0xFF5A5A78;

	/** Border color of a hovered button (lighter slate). */
	private static final int HOVER_BORDER = 0xFF9A9AC8;

	/** Fill of an idle button (near-black). */
	private static final int BACKGROUND = 0xB0101014;

	/** Fill of a hovered button (dark slate). */
	private static final int HOVER_BACKGROUND = 0xB028283C;

	private ButtonRenderer() {
	}

	/**
	 * Renders every enabled button and, when the cursor is on one, its
	 * tooltip.
	 *
	 * @param left    the container's left edge ({@code leftPos})
	 * @param top     the container's top edge ({@code topPos})
	 * @param width   the container's width ({@code imageWidth})
	 * @param height  the container's height ({@code imageHeight})
	 */
	public static void render(
			GuiGraphicsExtractor graphics,
			Font font,
			List<InventoryButton> buttons,
			int left,
			int top,
			int width,
			int height,
			int mouseX,
			int mouseY) {
		for (InventoryButton button : buttons) {
			if (!button.enabled()) {
				continue;
			}
			int x = ButtonGeometry.x(button, left, width);
			int y = ButtonGeometry.y(button, top, height);
			boolean hovered = ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE, mouseX, mouseY);

			drawBox(graphics, x, y, hovered);

			String iconId = button.icon();
			graphics.item(ButtonIcons.iconFor(iconId), x + 1, y + 1);

			if (hovered) {
				List<Component> lines = ButtonTooltips.lines(button, ButtonIcons.isKnown(iconId));
				graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
			}
		}
	}

	private static void drawBox(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
		graphics.fill(x, y, x + ButtonGeometry.SIZE, y + ButtonGeometry.SIZE, hovered ? HOVER_BORDER : BORDER);
		graphics.fill(x + 1, y + 1, x + ButtonGeometry.SIZE - 1, y + ButtonGeometry.SIZE - 1,
				hovered ? HOVER_BACKGROUND : BACKGROUND);
	}
}
