package net.zaminmc.zea.chat.render;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The chat tab strip's layout policy: how tab labels fit a maximum width.
 * A pure computation, so the behavior is pinned by tests without Minecraft.
 *
 * <p>Three stages, each only as far as needed: the natural layout (label
 * plus padding); then padding compressed toward {@code minPadding} — a
 * denser strip beats shortened labels; then labels shortened one
 * character at a time, longest tab first, the shortened form carrying an
 * ellipsis, never below one character. If even one-character labels with
 * minimal padding do not fit (an absurdly narrow maximum), the minimum
 * layout is returned overflowing rather than dropping tabs: every tab
 * stays visible and clickable, and the drawn extent always equals the
 * hit-test extent.
 *
 * <p>The strip is clamped to the chat panel's on-screen width. In 26.3
 * that width is {@code ChatComponent.getWidth(chatWidthOption)} —
 * {@code floor(option * 280 + 40)} screen pixels, independent of chat
 * scale, because the renderer scales by {@code 1/scale} internally
 * (verified against the 26.3 bytecode). An earlier note assumed the
 * on-screen width scaled with chat scale; it does not.
 *
 * @param widths    the drawn width of each tab, parallel to {@code labels}
 * @param labels    the label each tab should draw (with ellipsis when
 *                  shortened)
 * @param truncated whether each tab's label was shortened
 */
public record TabLayout(int[] widths, String[] labels, boolean[] truncated) {
	/** The ellipsis appended to shortened labels. */
	private static final String ELLIPSIS = "…";

	/** Sum of tab widths and gaps; the strip's full drawn extent. */
	public int total(int gap) {
		int sum = 0;
		for (int width : widths) {
			sum += width;
		}
		return sum + (widths.length > 1 ? gap * (widths.length - 1) : 0);
	}

	/**
	 * Lays out {@code labels} for a strip at most {@code maxWidth} wide.
	 *
	 * @param labels     the full tab labels, in display order
	 * @param maxWidth   the widest the strip may be
	 * @param padding    the natural inner padding of a tab, each side
	 * @param minPadding the floor padding compression stops at
	 * @param gap        pixels between tabs
	 * @param measure    how wide a string renders
	 */
	public static TabLayout compute(List<String> labels, int maxWidth, int padding, int minPadding,
			int gap, ToIntFunction<String> measure) {
		int count = labels.size();
		String[] current = labels.toArray(String[]::new);
		String[] display = labels.toArray(String[]::new);
		boolean[] shortened = new boolean[count];
		int[] textWidths = new int[count];
		for (int i = 0; i < count; i++) {
			textWidths[i] = measure.applyAsInt(current[i]);
		}

		int pad = padding;
		if (count > 0 && total(textWidths, pad, gap) > maxWidth) {
			// Compress the padding toward its floor before touching labels.
			int room = maxWidth - gaps(count, gap);
			int text = 0;
			for (int width : textWidths) {
				text += width;
			}
			pad = Math.max(minPadding, Math.min(padding, (room - text) / (2 * count)));
		}

		// Shorten the widest tab by one character until the strip fits.
		while (count > 0 && total(textWidths, pad, gap) > maxWidth) {
			int widest = -1;
			for (int i = 0; i < count; i++) {
				if (current[i].length() <= 1) {
					continue; // one character is the floor
				}
				if (widest < 0 || textWidths[i] > textWidths[widest]) {
					widest = i;
				}
			}
			if (widest < 0) {
				break; // everything is at the floor; overflow beats hiding tabs
			}
			current[widest] = current[widest].substring(0, current[widest].length() - 1);
			display[widest] = current[widest] + ELLIPSIS;
			shortened[widest] = true;
			textWidths[widest] = measure.applyAsInt(display[widest]);
		}

		int[] widths = new int[count];
		for (int i = 0; i < count; i++) {
			widths[i] = textWidths[i] + 2 * pad;
		}
		return new TabLayout(widths, display, shortened);
	}

	private static int total(int[] textWidths, int padding, int gap) {
		int sum = 0;
		for (int width : textWidths) {
			sum += width;
		}
		return sum + 2 * padding * textWidths.length + gaps(textWidths.length, gap);
	}

	private static int gaps(int count, int gap) {
		return count > 1 ? gap * (count - 1) : 0;
	}
}
