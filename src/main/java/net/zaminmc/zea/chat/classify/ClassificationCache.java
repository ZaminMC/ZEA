package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Optional;

/**
 * Remembers the classification of recently captured messages so the chat
 * GUI can filter by feed without re-parsing components. Entries are keyed
 * by component identity — EarthMC messages flow from capture to the chat
 * GUI as the same instance, and structurally equal components never merge
 * (a miss simply re-classifies).
 *
 * <p>Bounded: when the cap is reached the oldest entries (long since
 * scrolled away) are evicted. All access happens on the client thread
 * (chat events and chat rendering share it), so no synchronization.
 */
public final class ClassificationCache {
	private final ClassificationPipeline pipeline;
	private final IdentityHashMap<Component, ClassifiedMessage> entries = new IdentityHashMap<>();
	private final ArrayDeque<Component> insertionOrder = new ArrayDeque<>();
	private final int capacity;

	public ClassificationCache(ClassificationPipeline pipeline, int capacity) {
		if (pipeline == null) {
			throw new NullPointerException("pipeline must not be null");
		}
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.pipeline = pipeline;
		this.capacity = capacity;
	}

	/** Classifies and remembers a freshly captured message. */
	public ClassifiedMessage classify(CapturedMessage message) {
		ClassifiedMessage classified = pipeline.classify(message);
		put(message.original(), classified);
		return classified;
	}

	/** Remembers an already-computed classification. */
	public void remember(CapturedMessage message, ClassifiedMessage classified) {
		put(message.original(), classified);
	}

	/** The remembered classification of a displayed component, if classified before. */
	public Optional<ClassifiedMessage> lookup(Component content) {
		return Optional.ofNullable(entries.get(content));
	}

	/** The classification of a displayed component, classifying it on first sight. */
	public ClassifiedMessage classificationOf(Component content) {
		ClassifiedMessage cached = entries.get(content);
		if (cached != null) {
			return cached;
		}
		ClassifiedMessage classified = pipeline.classify(CapturedMessage.of(content));
		put(content, classified);
		return classified;
	}

	public int size() {
		return entries.size();
	}

	private void put(Component key, ClassifiedMessage value) {
		if (!entries.containsKey(key)) {
			insertionOrder.addLast(key);
		}
		entries.put(key, value);
		while (insertionOrder.size() > capacity) {
			entries.remove(insertionOrder.pollFirst());
		}
	}
}
