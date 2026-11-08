package net.zaminmc.zea.chat.feeds;

import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;

import java.util.Set;

/**
 * A client-side chat feed: a view selecting message categories. Feeds are
 * not server channels — a feed never implies a writable destination.
 *
 * @param id         stable identifier ("all", "global", ...)
 * @param label      display label
 * @param categories categories shown in this feed
 */
public record Feed(String id, String label, Set<MessageCategory> categories) {
	public Feed {
		if (id == null || label == null || categories == null || categories.isEmpty()) {
			throw new IllegalArgumentException("feed fields must be non-null and categories non-empty");
		}
		categories = Set.copyOf(categories);
	}

	public boolean shows(ClassifiedMessage message) {
		return categories.contains(message.category());
	}
}
