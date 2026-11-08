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
 * so buttons read as part of the GUI without new textures. In edit
 * mode, buttons draw with a gold border, disabled buttons stay visible
 * (dimmed), a ghost button offers creating new ones, and an in-progress
 * drag is drawn at its preview position.
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

	/** Border color while editing (gold). */
	private static final int EDIT_BORDER = 0xFFE8B14A;

	/** Overlay that dims disabled buttons in edit mode. */
	private static final int DISABLED_OVERLAY = 0x66101014;

	/** Anchor and offset of the "add button" ghost in edit mode: beside the top-left corner. */
	public static final ButtonAnchor GHOST_ANCHOR = ButtonAnchor.TOP_LEFT;
	public static final int GHOST_OFFSET_X = -24;
	public static final int GHOST_OFFSET_Y = 2;

	/** Where the edit-mode hint line sits above the container. */
	private static final int HINT_OFFSET_Y = -12;

	private ButtonRenderer() {
	}

	/**
	 * Renders the buttons, plus edit-mode furniture when requested.
	 *
	 * @param left     the container's left edge ({@code leftPos})
	 * @param top      the container's top edge ({@code topPos})
	 * @param width    the container's width ({@code imageWidth})
	 * @param height   the container's height ({@code imageHeight})
	 * @param editMode whether button editing is active
	 * @param drag     the edit-mode drag session (idle when nothing is dragged)
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
			int mouseY,
			boolean editMode,
			ButtonDragSession drag) {
		boolean dragging = drag.isActive();
		for (InventoryButton button : buttons) {
			if (!button.enabled() && !editMode) {
				continue;
			}
			boolean isDragged = dragging && button.id().equals(drag.button().id());
			int x = isDragged ? drag.previewX() : ButtonGeometry.x(button, left, width);
			int y = isDragged ? drag.previewY() : ButtonGeometry.y(button, top, height);
			boolean hovered = !dragging
					&& ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE, mouseX, mouseY);

			drawBox(graphics, x, y, hovered, editMode);

			String iconId = button.icon();
			graphics.item(ButtonIcons.iconFor(iconId), x + 1, y + 1);
			if (!button.enabled()) {
				graphics.fill(x, y, x + ButtonGeometry.SIZE, y + ButtonGeometry.SIZE, DISABLED_OVERLAY);
			}

			if (hovered) {
				List<Component> lines = ButtonTooltips.lines(button, ButtonIcons.isKnown(iconId));
				graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
			}
		}

		if (editMode) {
			renderGhost(graphics, font, left, top, width, mouseX, mouseY);
			graphics.text(
					font,
					Component.translatable("zea.buttons.editing_hint"),
					left,
					top + HINT_OFFSET_Y,
					0xFFFFFFFF,
					true);
		}
	}

	/** The dashed-in-spirit "add button" affordance: a box with a plus. */
	private static void renderGhost(GuiGraphicsExtractor graphics, Font font, int left, int top, int width,
			int mouseX, int mouseY) {
		int x = ButtonGeometry.x(GHOST_ANCHOR, GHOST_OFFSET_X, left, width);
		int y = ButtonGeometry.y(GHOST_ANCHOR, GHOST_OFFSET_Y, top, 0);
		boolean hovered = ButtonGeometry.contains(x, y, ButtonGeometry.SIZE, ButtonGeometry.SIZE, mouseX, mouseY);

		drawBox(graphics, x, y, hovered, true);
		graphics.centeredText(
				font,
				"+",
				x + ButtonGeometry.SIZE / 2,
				y + (ButtonGeometry.SIZE - font.lineHeight) / 2,
				hovered ? 0xFFFFFFFF : 0xFF9A9AC8);
		if (hovered) {
			graphics.setComponentTooltipForNextFrame(
					font,
					List.of(Component.translatable("zea.buttons.add")),
					mouseX,
					mouseY);
		}
	}

	private static void drawBox(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, boolean editMode) {
		int border = editMode ? EDIT_BORDER : hovered ? HOVER_BORDER : BORDER;
		int background = hovered ? HOVER_BACKGROUND : BACKGROUND;
		graphics.fill(x, y, x + ButtonGeometry.SIZE, y + ButtonGeometry.SIZE, border);
		graphics.fill(x + 1, y + 1, x + ButtonGeometry.SIZE - 1, y + ButtonGeometry.SIZE - 1, background);
	}
}
