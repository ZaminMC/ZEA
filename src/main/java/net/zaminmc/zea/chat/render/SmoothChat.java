package net.zaminmc.zea.chat.render;

/**
 * The smooth-chat animation state: one vertical offset, measured in
 * unscaled chat units (line heights), that is set when the chat's scroll
 * anchor jumps and eases to zero afterwards. While non-zero, rendering
 * translates the chat by the eased remainder so the jump looks like a
 * glide.
 *
 * <p>Easing is quartic ease-out: fast at first, settling gently. A
 * discontinuity arriving mid-animation adds to the remaining offset and
 * restarts the clock, so consecutive jumps never fight each other.
 *
 * <p>Offsets are unscaled chat units; the renderer converts to screen
 * pixels with the chat scale. Duration is passed in per call so live
 * configuration changes apply mid-animation; zero disables the effect
 * (offsets collapse immediately).
 */
public final class SmoothChat {
	private long startMillis;
	private double offsetUnits;

	/**
	 * Records a scroll discontinuity.
	 *
	 * @param units how far content should start below its new anchor
	 *              (positive = lower); {@code (oldAnchor - newAnchor) * lineHeight}
	 * @param nowMillis the current time
	 * @param durationMillis animation length; zero disables animation
	 */
	public void shift(double units, long nowMillis, int durationMillis) {
		if (units == 0) {
			return;
		}
		offsetUnits = easedOffset(nowMillis, durationMillis) + units;
		startMillis = nowMillis;
	}

	/**
	 * The offset to apply at render time: the eased remainder, exactly
	 * zero once the animation has finished (or when disabled).
	 */
	public double offsetAt(long nowMillis, int durationMillis) {
		return easedOffset(nowMillis, durationMillis);
	}

	/** Quartic ease-out: {@code 1 - (1 - t)^4}. */
	static double easeOutQuartic(double t) {
		double remaining = 1 - t;
		return 1 - remaining * remaining * remaining * remaining;
	}

	private double easedOffset(long nowMillis, int durationMillis) {
		if (durationMillis <= 0 || offsetUnits == 0) {
			offsetUnits = 0;
			return 0;
		}
		long elapsed = nowMillis - startMillis;
		if (elapsed <= 0) {
			return offsetUnits;
		}
		double t = (double) elapsed / durationMillis;
		if (t >= 1) {
			offsetUnits = 0;
			return 0;
		}
		return offsetUnits * (1 - easeOutQuartic(t));
	}
}
