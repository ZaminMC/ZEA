package net.zaminmc.zea.chat.feeds;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.zaminmc.zea.chat.classify.ClassificationCache;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * Applies the selected display feed — and, while a chat search is open,
 * its query — to the vanilla chat as its official
 * {@code visibleMessageFilter}. Filtering is visual only — every message
 * stays in the chat history, so switching back to the All tab (or any
 * other feed) shows them again.
 *
 * <p>The All tab contributes no feed predicate of its own, which also
 * restores messages ZEA never classified (shown by vanilla as usual); a
 * search, however, filters even on All.
 *
 * <p>The search query is held here — not in the chat screen — so it is
 * the single source of truth that survives screen rebuilds (window
 * resize) and can be reset on disconnect. {@code null} means the search
 * is closed; an empty string means open but not narrowing anything yet.
 */
public final class FeedFilter {
	private final ClassificationCache cache;
	private final FeedSelection selection;
	private String searchQuery;

	public FeedFilter(ClassificationCache cache, FeedSelection selection) {
		this.cache = cache;
		this.selection = selection;
	}

	/** The active search query, or {@code null} when the search is closed. */
	public String searchQuery() {
		return searchQuery;
	}

	/**
	 * Sets the search query. {@code null} closes the search; a blank
	 * query keeps it open without filtering. Callers re-apply the filter
	 * through {@link #apply(ChatComponent)} for the change to take
	 * effect.
	 */
	public void setSearchQuery(String query) {
		this.searchQuery = query;
	}

	/**
	 * The predicate for the selected feed composed with the search
	 * query, or {@code null} when neither filters anything.
	 */
	public Predicate<GuiMessage> currentFilter() {
		Predicate<GuiMessage> byFeed = feedPredicate();
		Predicate<GuiMessage> bySearch = searchPredicate(searchQuery);
		if (byFeed == null) {
			return bySearch;
		}
		if (bySearch == null) {
			return byFeed;
		}
		return byFeed.and(bySearch);
	}

	/** Installs (or clears) the filter on the live chat component. */
	public void apply(ChatComponent chat) {
		chat.setVisibleMessageFilter(currentFilter());
	}

	private Predicate<GuiMessage> feedPredicate() {
		Feed feed = selection.selected();
		if (feed.showsEverything()) {
			return null;
		}
		return message -> feed.shows(cache.classificationOf(message.content()));
	}

	/** Case-insensitive substring match over the flattened message text. */
	private static Predicate<GuiMessage> searchPredicate(String query) {
		if (query == null || query.isBlank()) {
			return null;
		}
		String needle = query.toLowerCase(Locale.ROOT);
		return message -> message.content().getString().toLowerCase(Locale.ROOT).contains(needle);
	}
}
