package net.zaminmc.zea.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Memoizes {@link GuiMessage#splitLines(Font, int)} per message. Vanilla
 * recomputes the wrap on every call — correct for single appends, ruinous
 * for {@code ChatComponent.refreshTrimmedMessages}, which re-adds the whole
 * history on every filter change. With a chat-history mod widening the
 * store past vanilla's 100 messages, each tab switch re-wrapped every
 * stored message (font measurement plus allocation per line): the
 * live-reported lag and freeze while switching feeds. A wrap depends only
 * on the message (immutable record), the font and the width, so it is
 * computed once per message and reused by every later rebuild; the dev.24
 * live session still re-split on every switch.
 *
 * <p>Entries are keyed by message identity and stamped with the font and
 * width they were computed for — a resource-reload font or a chat
 * width/scale change misses and recomputes. Bounded FIFO like the
 * classification cache: the oldest entries (long scrolled away) evict
 * first. All access happens on the client thread, so no synchronization.
 */
public final class SplitLineCache {
	private record Entry(Font font, int maxWidth, List<FormattedCharSequence> lines) {
	}

	private final IdentityHashMap<GuiMessage, Entry> entries = new IdentityHashMap<>();
	private final ArrayDeque<GuiMessage> insertionOrder = new ArrayDeque<>();
	private final int capacity;

	public SplitLineCache(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.capacity = capacity;
	}

	/** The memoized split for this message at this font and width, or null. */
	public List<FormattedCharSequence> cached(GuiMessage message, Font font, int maxWidth) {
		Entry entry = entries.get(message);
		if (entry != null && entry.font() == font && entry.maxWidth() == maxWidth) {
			return entry.lines();
		}
		return null;
	}

	/** Stores a freshly computed split for later rebuilds. */
	public void remember(GuiMessage message, Font font, int maxWidth, List<FormattedCharSequence> lines) {
		if (!entries.containsKey(message)) {
			insertionOrder.addLast(message);
		}
		entries.put(message, new Entry(font, maxWidth, lines));
		while (insertionOrder.size() > capacity) {
			entries.remove(insertionOrder.pollFirst());
		}
	}

	/** Drops everything; called when the chat itself forgets its messages. */
	public void clear() {
		entries.clear();
		insertionOrder.clear();
	}

	/** The number of memoized messages; the test surface. */
	public int size() {
		return entries.size();
	}
}
