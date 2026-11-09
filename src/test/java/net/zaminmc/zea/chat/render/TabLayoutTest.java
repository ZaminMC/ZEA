package net.zaminmc.zea.chat.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tab strip's layout policy: natural fit, padding compression before
 * label shortening, longest-first shortening with ellipsis, the one
 * character floor, and the documented overflow when nothing more can
 * give. The measurer is a fake (every character 6 px) so the tests pin
 * the arithmetic, not a font.
 */
class TabLayoutTest {
	private static final int PADDING = 5;
	private static final int MIN_PADDING = 1;
	private static final int GAP = 1;
	private static final int CHAR = 6;

	private static int measure(String text) {
		return text.length() * CHAR;
	}

	@Test
	void naturalLayoutWhenItFits() {
		TabLayout layout = TabLayout.compute(List.of("All", "Global"), 200,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertFalse(layout.truncated()[0]);
		assertEquals("All", layout.labels()[0]);
		assertEquals("Global", layout.labels()[1]);
		assertEquals(3 * CHAR + 2 * PADDING, layout.widths()[0]);
		assertEquals(6 * CHAR + 2 * PADDING, layout.widths()[1]);
		assertEquals(3 * CHAR + 6 * CHAR + 2 * (2 * PADDING) + GAP, layout.total(GAP));
	}

	@Test
	void paddingCompressesBeforeLabelsShorten() {
		// Text is 54 px, gaps 1; 66 leaves 11 px of padding across 2 tabs:
		// floor(11 / 4) = 2 px per side, above the floor — no truncation.
		TabLayout layout = TabLayout.compute(List.of("All", "Global"), 66,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertFalse(layout.truncated()[0]);
		assertFalse(layout.truncated()[1]);
		assertEquals("All", layout.labels()[0]);
		assertEquals("Global", layout.labels()[1]);
		assertEquals(3 * CHAR + 2 * 2, layout.widths()[0]);
		assertEquals(6 * CHAR + 2 * 2, layout.widths()[1]);
		assertEquals(54 + 4 * 2 + GAP, layout.total(GAP)); // 63 <= 66
		assertTrue(layout.total(GAP) <= 66);
	}

	@Test
	void paddingStopsAtItsFloor() {
		// 58 leaves no room for even 2 px per side (Java's truncating
		// division of -3/4 is 0): the floor of 1 applies.
		TabLayout layout = TabLayout.compute(List.of("All", "Global"), 58,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertEquals(3 * CHAR + 2 * MIN_PADDING, layout.widths()[0]);
	}

	@Test
	void longestLabelShortensFirst() {
		// At 52 px not even floor padding fits the text (59), so "Global"
		// gives way — each step trades a character for the ellipsis, so it
		// takes three steps ("Glo…", 4 chars = 24 px) to fit — while "All"
		// stays intact.
		TabLayout layout = TabLayout.compute(List.of("All", "Global"), 52,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertFalse(layout.truncated()[0]);
		assertEquals("All", layout.labels()[0]);
		assertTrue(layout.truncated()[1]);
		assertEquals("Glo…", layout.labels()[1]);
		assertEquals(4 * CHAR, layout.widths()[1] - 2 * MIN_PADDING);
		assertEquals(3 * CHAR + 4 * CHAR + 2 * (2 * MIN_PADDING) + GAP, layout.total(GAP)); // 47
	}

	@Test
	void shorteningNeverGoesBelowOneCharacter() {
		// Absurdly narrow: every label bottoms out at one character plus
		// ellipsis; nothing is dropped, even though the strip overflows.
		TabLayout layout = TabLayout.compute(List.of("All", "Global", "Trade"), 20,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		for (int i = 0; i < 3; i++) {
			assertTrue(layout.truncated()[i]);
			assertEquals(2, layout.labels()[i].length()); // one character + ellipsis
			assertEquals(2 * CHAR + 2 * MIN_PADDING, layout.widths()[i]);
		}
		// The documented overflow: three 14 px tabs plus gaps exceed 20 px.
		assertTrue(layout.total(GAP) > 20);
	}

	@Test
	void singleCharacterLabelsAreNeverShortened() {
		TabLayout layout = TabLayout.compute(List.of("A", "B"), 8,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertEquals("A", layout.labels()[0]);
		assertEquals("B", layout.labels()[1]);
		assertFalse(layout.truncated()[0]);
		assertFalse(layout.truncated()[1]);
	}

	@Test
	void noTabsLayOutEmpty() {
		TabLayout layout = TabLayout.compute(List.of(), 100,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertEquals(0, layout.widths().length);
		assertEquals(0, layout.total(GAP));
	}

	@Test
	void singleTabHasNoGaps() {
		TabLayout layout = TabLayout.compute(List.of("Global"), 100,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertEquals(6 * CHAR + 2 * PADDING, layout.total(GAP));
	}

	@Test
	void shorteningTakesFromTheWidestTabEachStep() {
		// "Crate Wins" (10 chars) is wider than "General" (7); with a 100 px
		// budget the padding bottoms out at 107 px, so "Crate Wins" loses
		// three characters (each loss must outrun the ellipsis it gains)
		// while "General" loses none.
		TabLayout layout = TabLayout.compute(List.of("General", "Crate Wins"), 100,
				PADDING, MIN_PADDING, GAP, TabLayoutTest::measure);

		assertFalse(layout.truncated()[0]);
		assertEquals("General", layout.labels()[0]);
		assertTrue(layout.truncated()[1]);
		assertEquals("Crate W…", layout.labels()[1]);
		assertTrue(layout.total(GAP) <= 100);
	}
}
