package net.zaminmc.zea.mention;

import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Remembers which captured messages contain a mention, so the chat
 * renderer can tint their lines. Keyed by component identity — the
 * message flows from capture to the chat GUI as the same instance, and a
 * miss simply means no tint.
 *
 * <p>Bounded: when the cap is reached the oldest entries (long since
 * scrolled away) are evicted. All access happens on the client thread
 * (chat events and chat rendering share it).
 */
public final class MentionHighlight {
        /** Comfortably above the chat's 100 trimmed lines. */
        private static final int DEFAULT_CAPACITY = 128;

        private final Set<Component> highlighted = Collections.newSetFromMap(new IdentityHashMap<>());
        private final ArrayDeque<Component> insertionOrder = new ArrayDeque<>();
        private final int capacity;

        public MentionHighlight() {
                this(DEFAULT_CAPACITY);
        }

        public MentionHighlight(int capacity) {
                if (capacity <= 0) {
                        throw new IllegalArgumentException("capacity must be positive");
                }
                this.capacity = capacity;
        }

        /** Marks a message's component as containing a mention. */
        public void remember(Component message) {
                if (message == null || highlighted.contains(message)) {
                        return;
                }
                highlighted.add(message);
                insertionOrder.addLast(message);
                while (insertionOrder.size() > capacity) {
                        highlighted.remove(insertionOrder.pollFirst());
                }
        }

        /** Whether this exact component carries a mention. */
        public boolean isHighlighted(Component message) {
                return highlighted.contains(message);
        }

        /** Forgets everything; the disconnect path. */
        public void clear() {
                highlighted.clear();
                insertionOrder.clear();
        }

        /** How many messages are currently marked. */
        public int size() {
                return highlighted.size();
        }
}
