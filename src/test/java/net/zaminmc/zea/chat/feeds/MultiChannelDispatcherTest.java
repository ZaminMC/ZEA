package net.zaminmc.zea.chat.feeds;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiChannelDispatcherTest {
	private static final long INTERVAL = 1000;

	/** Records sends; can be told to fail with an exception. */
	private static final class RecordingSender implements MultiChannelDispatcher.Sender {
		final List<OutgoingMessage> sent = new ArrayList<>();
		Exception failWith;

		@Override
		public void send(OutgoingMessage message) throws Exception {
			if (failWith != null) {
				throw failWith;
			}
			sent.add(message);
		}
	}

	/** Records listener callbacks. */
	private static final class RecordingListener implements MultiChannelDispatcher.Listener {
		final List<String> events = new ArrayList<>();

		@Override
		public void onDispatched(OutgoingMessage message, int remaining) {
			events.add("dispatched:" + message.command() + ":" + remaining);
		}

		@Override
		public void onFailed(OutgoingMessage message, Throwable cause, int cancelled) {
			events.add("failed:" + message.command() + ":" + cancelled);
		}

		@Override
		public void onCompleted() {
			events.add("completed");
		}

		@Override
		public void onCancelled(int remaining) {
			events.add("cancelled:" + remaining);
		}
	}

	private static OutgoingMessage message(String command) {
		return new OutgoingMessage(command, "hello");
	}

	private static List<OutgoingMessage> batch(String... commands) {
		List<OutgoingMessage> batch = new ArrayList<>();
		for (String command : commands) {
			batch.add(message(command));
		}
		return batch;
	}

	@Test
	void sendsMessagesOnePerInterval() {
		RecordingSender sender = new RecordingSender();
		RecordingListener listener = new RecordingListener();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, listener, INTERVAL);

		dispatcher.dispatch(batch("g", "tc", "nc"));
		dispatcher.tick(0);
		dispatcher.tick(INTERVAL - 1); // too early: nothing sent
		dispatcher.tick(INTERVAL);
		dispatcher.tick(INTERVAL + 1); // too early
		dispatcher.tick(2 * INTERVAL);

		assertEquals(List.of("g", "tc", "nc"), sender.sent.stream().map(OutgoingMessage::command).toList());
		assertEquals(List.of("dispatched:g:2", "dispatched:tc:1", "completed"), listener.events);
	}

	@Test
	void firstMessageSendsImmediately() {
		RecordingSender sender = new RecordingSender();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, new RecordingListener(), INTERVAL);

		dispatcher.dispatch(batch("g", "tc"));
		dispatcher.tick(500);

		assertEquals(1, sender.sent.size());
	}

	@Test
	void failureCancelsRemainingAndReports() {
		RecordingSender sender = new RecordingSender();
		RecordingListener listener = new RecordingListener();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, listener, INTERVAL);

		dispatcher.dispatch(batch("g", "tc", "nc"));
		dispatcher.tick(0);
		sender.failWith = new IllegalStateException("disconnected");
		dispatcher.tick(INTERVAL);

		assertEquals(List.of("g"), sender.sent.stream().map(OutgoingMessage::command).toList());
		assertEquals(List.of("dispatched:g:2", "failed:tc:1"), listener.events);
		assertFalse(dispatcher.isDispatching());
	}

	@Test
	void cancelStopsDispatch() {
		RecordingSender sender = new RecordingSender();
		RecordingListener listener = new RecordingListener();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, listener, INTERVAL);

		dispatcher.dispatch(batch("g", "tc", "nc"));
		dispatcher.tick(0);
		dispatcher.cancel();
		dispatcher.tick(INTERVAL);
		dispatcher.tick(2 * INTERVAL);

		assertEquals(1, sender.sent.size());
		assertEquals(List.of("dispatched:g:2", "cancelled:2"), listener.events);
	}

	@Test
	void newBatchCancelsTheOldOne() {
		RecordingSender sender = new RecordingSender();
		RecordingListener listener = new RecordingListener();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, listener, INTERVAL);

		dispatcher.dispatch(batch("g", "tc"));
		dispatcher.tick(0);
		dispatcher.dispatch(batch("nc"));
		dispatcher.tick(INTERVAL);

		assertEquals(List.of("g", "nc"), sender.sent.stream().map(OutgoingMessage::command).toList());
		assertTrue(listener.events.contains("cancelled:1"));
		assertTrue(listener.events.contains("completed"));
	}

	@Test
	void emptyBatchIsIgnored() {
		RecordingSender sender = new RecordingSender();
		RecordingListener listener = new RecordingListener();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, listener, INTERVAL);

		dispatcher.dispatch(List.of());
		dispatcher.tick(0);

		assertTrue(sender.sent.isEmpty());
		assertTrue(listener.events.isEmpty());
		assertFalse(dispatcher.isDispatching());
	}

	@Test
	void consecutiveBatchesAlsoRespectTheInterval() {
		RecordingSender sender = new RecordingSender();
		MultiChannelDispatcher dispatcher = new MultiChannelDispatcher(sender, new RecordingListener(), INTERVAL);

		dispatcher.dispatch(batch("g"));
		dispatcher.tick(0);
		dispatcher.dispatch(batch("tc"));
		dispatcher.tick(1); // first of the new batch is still spaced from the previous send

		assertEquals(1, sender.sent.size());
	}
}
