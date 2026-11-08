package net.zaminmc.zea.mention;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The highlight set: identity keying, bounded eviction and clearing.
 */
class MentionHighlightTest {
        @Test
        void remembersByIdentityNotEquality() {
                MentionHighlight highlight = new MentionHighlight();
                Component first = Component.literal("hey Zamin");
                Component equal = Component.literal("hey Zamin");

                highlight.remember(first);

                assertTrue(highlight.isHighlighted(first));
                assertFalse(highlight.isHighlighted(equal), "structurally equal is not the same message");
        }

        @Test
        void rememberingTwiceKeepsOneEntry() {
                MentionHighlight highlight = new MentionHighlight();
                Component message = Component.literal("hey Zamin");

                highlight.remember(message);
                highlight.remember(message);

                assertEquals(1, highlight.size());
        }

        @Test
        void evictsOldestWhenFull() {
                MentionHighlight highlight = new MentionHighlight(2);
                Component first = Component.literal("one");
                Component second = Component.literal("two");
                Component third = Component.literal("three");

                highlight.remember(first);
                highlight.remember(second);
                highlight.remember(third);

                assertEquals(2, highlight.size());
                assertFalse(highlight.isHighlighted(first));
                assertTrue(highlight.isHighlighted(second));
                assertTrue(highlight.isHighlighted(third));
        }

        @Test
        void nullIsIgnored() {
                MentionHighlight highlight = new MentionHighlight();

                highlight.remember(null);

                assertEquals(0, highlight.size());
        }

        @Test
        void clearEmptiesEverything() {
                MentionHighlight highlight = new MentionHighlight();
                highlight.remember(Component.literal("hey Zamin"));

                highlight.clear();

                assertEquals(0, highlight.size());
        }

        @Test
        void nonPositiveCapacityIsRejected() {
                assertThrows(IllegalArgumentException.class, () -> new MentionHighlight(0));
        }
}
