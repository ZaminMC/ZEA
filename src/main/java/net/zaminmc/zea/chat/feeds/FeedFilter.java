package net.zaminmc.zea.chat.feeds;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.zaminmc.zea.chat.classify.ClassificationCache;

import java.util.function.Predicate;

/**
 * Applies the selected display feed to the vanilla chat as its official
 * {@code visibleMessageFilter}. Filtering is visual only — every message
 * stays in the chat history, so switching back to the All tab (or any
 * other feed) shows them again.
 *
 * <p>The All tab clears the filter entirely, which also restores messages
 * ZEA never classified (shown by vanilla as usual).
 */
public final class FeedFilter {
	private final ClassificationCache cache;
	private final FeedSelection selection;

	public FeedFilter(ClassificationCache cache, FeedSelection selection) {
		this.cache = cache;
		this.selection = selection;
	}

	/** The predicate for the selected feed, or {@code null} for All. */
	public Predicate<GuiMessage> currentFilter() {
		Feed feed = selection.selected();
		if (feed.showsEverything()) {
			return null;
		}
		return message -> feed.shows(cache.classificationOf(message.content()));
	}

	/** Installs (or clears) the filter on the live chat component. */
	public void apply(ChatComponent chat) {
		chat.setVisibleMessageFilter(currentFilter());
	}
}
