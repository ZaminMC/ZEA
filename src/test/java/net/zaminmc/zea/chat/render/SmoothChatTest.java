package net.zaminmc.zea.chat.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothChatTest {
	private static final int DURATION = 200;

	@Test
	void offsetStartsAtFullValue() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(3 * 10, 1000, DURATION);

		assertEquals(30.0, smooth.offsetAt(1000, DURATION), 1e-9);
	}

	@Test
	void negativeShiftsStartAtFullValueToo() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(-2 * 10, 0, DURATION);

		assertEquals(-20.0, smooth.offsetAt(0, DURATION), 1e-9);
	}

	@Test
	void offsetEndsAtExactlyZero() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(30, 1000, DURATION);

		assertEquals(0.0, smooth.offsetAt(1000 + DURATION, DURATION), 0.0);
		// And stays zero afterwards.
		assertEquals(0.0, smooth.offsetAt(1000 + DURATION + 5000, DURATION), 0.0);
	}

	@Test
	void offsetEasesOutQuartically() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(40, 0, DURATION);

		double quarter = smooth.offsetAt(DURATION / 4, DURATION);
		double half = smooth.offsetAt(DURATION / 2, DURATION);

		// Remaining fraction is (1 - t)^4: 0.75^4 and 0.5^4.
		assertEquals(40 * 0.31640625, quarter, 1e-9);
		assertEquals(40 * 0.0625, half, 1e-9);
		// Fast start, gentle settle.
		assertTrue(quarter < 40 * 0.4 && quarter > 40 * 0.25, "quarter: " + quarter);
		assertTrue(half < quarter);
	}

	@Test
	void midFlightShiftAccumulatesFromTheRemainder() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(20, 0, DURATION);
		double remainingAtHalf = 20 * 0.0625;

		smooth.shift(10, DURATION / 2, DURATION);

		assertEquals(remainingAtHalf + 10, smooth.offsetAt(DURATION / 2, DURATION), 1e-9);
	}

	@Test
	void zeroDurationCollapsesImmediately() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(30, 1000, 0);

		assertEquals(0.0, smooth.offsetAt(1000, 0), 0.0);
	}

	@Test
	void zeroShiftIsANoOp() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(30, 0, DURATION);
		smooth.shift(0, DURATION / 4, DURATION);

		// No restart: the clock still began at 0.
		assertEquals(30 * 0.31640625, smooth.offsetAt(DURATION / 4, DURATION), 1e-9);
	}

	@Test
	void finishedAnimationIsForgotten() {
		SmoothChat smooth = new SmoothChat();
		smooth.shift(30, 0, DURATION);
		smooth.offsetAt(DURATION, DURATION);

		// After finishing, a new shift starts from zero remainder.
		smooth.shift(10, DURATION + 5, DURATION);
		assertEquals(10.0, smooth.offsetAt(DURATION + 5, DURATION), 1e-9);
	}
}
