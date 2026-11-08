package net.zaminmc.zea.chat.feeds;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which feed is displayed. One of the three deliberately separate states:
 * display selection (this), server writing channel ({@link ChannelState}),
 * and multi-send targets ({@link SendTargets}). They must never collapse
 * into one variable.
 */
public final class FeedSelection {
	private Feed selected;

	public FeedSelection(Feed initial) {
		if (initial == null) {
			throw new NullPointerException("initial feed must not be null");
		}
		this.selected = initial;
	}

	public Feed selected() {
		return selected;
	}

	public void select(Feed feed) {
		if (feed == null) {
			throw new NullPointerException("feed must not be null");
		}
		selected = feed;
	}
}
