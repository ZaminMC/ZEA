package net.zaminmc.zea.chat.feeds;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes outgoing messages to writable channels using the verified
 * per-message command form ({@code /g <message>}). Routing a message
 * through a channel never changes the server's writing channel — that is
 * tracked separately by {@link ChannelState} from server reports.
 */
public final class OutgoingRouter {
	/**
	 * Routes one message to multiple destinations, in selection order.
	 * The caller dispatches them with a conservative interval
	 * ({@link MultiChannelDispatcher}); they are never sent in the same
	 * instant.
	 */
	public List<OutgoingMessage> route(Iterable<WriteChannel> targets, String message) {
		if (targets == null || message == null) {
			throw new NullPointerException("targets and message must not be null");
		}
		List<OutgoingMessage> outgoing = new ArrayList<>();
		for (WriteChannel target : targets) {
			outgoing.add(route(target, message));
		}
		return List.copyOf(outgoing);
	}

	public OutgoingMessage route(WriteChannel channel, String message) {
		if (channel == null || message == null) {
			throw new NullPointerException("channel and message must not be null");
		}
		if (message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
		return new OutgoingMessage(channel.messageCommand(), message);
	}
}
