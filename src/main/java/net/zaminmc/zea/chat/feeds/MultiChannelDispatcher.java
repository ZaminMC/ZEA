package net.zaminmc.zea.chat.feeds;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative multi-channel dispatch: pending messages are sent one at a
 * time, separated by a configurable interval, so server spam restrictions
 * are never bypassed and automated sending is not disguised as human
 * typing (timing is uniform and documented, never randomized).
 *
 * <p>A state machine driven by {@link #tick(long)} — the client wiring
 * calls it once per client tick. Dispatch stops at the first failure
 * (remaining messages are cancelled and the failure is reported) and can
 * be cancelled at any time (e.g. on disconnect).
 */
public final class MultiChannelDispatcher {
	/** Listener for dispatch progress; called on the driving thread. */
	public interface Listener {
		default void onDispatched(OutgoingMessage message, int remaining) {
		}

		default void onFailed(OutgoingMessage message, Throwable cause, int cancelled) {
		}

		default void onCompleted() {
		}

		default void onCancelled(int remaining) {
		}
	}

	/** Sends one command. Returns normally on success; throws on failure. */
	@FunctionalInterface
	public interface Sender {
		void send(OutgoingMessage message) throws Exception;
	}

	private final Sender sender;
	private final Listener listener;
	private final long intervalMillis;

	private final List<OutgoingMessage> pending = new ArrayList<>();
	private long lastDispatchAt = Long.MIN_VALUE;

	public MultiChannelDispatcher(Sender sender, Listener listener, long intervalMillis) {
		if (sender == null || listener == null) {
			throw new NullPointerException("sender and listener must not be null");
		}
		if (intervalMillis < 0) {
			throw new IllegalArgumentException("interval must not be negative");
		}
		this.sender = sender;
		this.listener = listener;
		this.intervalMillis = intervalMillis;
	}

	/** Offers a batch for staggered dispatch; the first message goes out on the next tick. */
	public void dispatch(List<OutgoingMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			return;
		}
		cancel(pending.size());
		pending.addAll(messages);
	}

	/** Cancels remaining dispatches (disconnect, user cancel). */
	public void cancel() {
		cancel(pending.size());
	}

	private void cancel(int remaining) {
		if (remaining > 0) {
			pending.clear();
			listener.onCancelled(remaining);
		}
	}

	public boolean isDispatching() {
		return !pending.isEmpty();
	}

	public int pendingCount() {
		return pending.size();
	}

	/** Advances dispatch; {@code nowMillis} comes from the driving clock. */
	public void tick(long nowMillis) {
		if (pending.isEmpty()) {
			return;
		}
		if (lastDispatchAt != Long.MIN_VALUE && nowMillis - lastDispatchAt < intervalMillis) {
			return;
		}
		OutgoingMessage message = pending.get(0);
		try {
			sender.send(message);
		} catch (Exception | LinkageError cause) {
			int cancelled = pending.size() - 1;
			pending.clear();
			listener.onFailed(message, cause, cancelled);
			return;
		}
		pending.remove(0);
		lastDispatchAt = nowMillis;
		if (pending.isEmpty()) {
			listener.onCompleted();
		} else {
			listener.onDispatched(message, pending.size());
		}
	}

	/** Interval between dispatches, in milliseconds. */
	public long intervalMillis() {
		return intervalMillis;
	}
}
