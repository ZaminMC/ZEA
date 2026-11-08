package net.zaminmc.zea.chat.feeds;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The multi-send destinations chosen with shift-click. One of the three
 * deliberately separate states (see {@link FeedSelection}). Selection is
 * an explicit multi-target operation — it is not the meaning of the All
 * tab and never invents a send-to-all capability.
 */
public final class SendTargets {
	private final Set<WriteChannel> targets = new LinkedHashSet<>();

	/** Toggles a channel in the selection. */
	public boolean toggle(WriteChannel channel) {
		if (channel == null) {
			throw new NullPointerException("channel must not be null");
		}
		if (targets.contains(channel)) {
			targets.remove(channel);
			return false;
		}
		targets.add(channel);
		return true;
	}

	public boolean isSelected(WriteChannel channel) {
		return targets.contains(channel);
	}

	public void clear() {
		targets.clear();
	}

	public boolean isEmpty() {
		return targets.isEmpty();
	}

	public Set<WriteChannel> targets() {
		return Collections.unmodifiableSet(targets);
	}
}
