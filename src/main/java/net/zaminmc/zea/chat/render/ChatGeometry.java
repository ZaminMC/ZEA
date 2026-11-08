package net.zaminmc.zea.chat.render;

/**
 * The vanilla chat line geometry, mirrored from ChatComponent's own
 * formulas (Minecraft 26.3) so hit-testing lands on the same line the
 * player sees. All inputs and outputs are in unscaled chat coordinates;
 * callers convert screen coordinates with {@code local = mouse / scale}.
 *
 * <p>Kept in sync with vanilla's rendering math:
 * {@code chatBottom = floor((guiHeight - 40) / scale)},
 * {@code entryHeight = (int)(9 * (1 + chatLineSpacing))},
 * {@code entryBottomToMessageY = round(8 * (1 + spacing) - 4 * spacing)}.
 * A line's text occupies {@code [chatBottom - slot * entryHeight - entryBottomToMessageY,
 * + 9)}; the hit test matches those text regions.
 */
public final class ChatGeometry {
	/** Pixels between the screen bottom and the lowest chat entry (unscaled). */
	public static final int BOTTOM_ZONE = 40;
	/** Font line height; vanilla hardcodes 9 for chat. */
	public static final int FONT_HEIGHT = 9;

	private ChatGeometry() {
	}

	/** The unscaled y where chat entries end (bottom of the lowest entry). */
	public static int chatBottom(int guiHeight, double scale) {
		return (int) Math.floor((guiHeight - BOTTOM_ZONE) / scale);
	}

	/** Height of one chat entry, including the spacing option. */
	public static int entryHeight(double chatLineSpacing) {
		return (int) (FONT_HEIGHT * (1 + chatLineSpacing));
	}

	/** Distance from an entry's bottom edge up to its text top. */
	public static int entryBottomToMessageY(double chatLineSpacing) {
		return (int) Math.round(8 * (1 + chatLineSpacing) - 4 * chatLineSpacing);
	}

	/**
	 * The on-screen line slot under the cursor, counted from the bottom
	 * (0 = lowest visible line), or -1 when the cursor is outside the
	 * text area (input strip, side margins, or below the lowest line).
	 * Slots above the panel top are returned for callers to clamp.
	 */
	public static int lineSlotAt(double localX, double localY, int chatBottom, int chatWidth,
			int entryHeight, int entryBottomToMessageY) {
		if (localX < 0 || localX >= chatWidth) {
			return -1;
		}
		// Top of the lowest line's text region.
		int lowestTextTop = chatBottom - entryBottomToMessageY;
		if (localY >= lowestTextTop + FONT_HEIGHT) {
			return -1;
		}
		return (int) Math.ceil((lowestTextTop - localY) / (double) entryHeight);
	}
}
