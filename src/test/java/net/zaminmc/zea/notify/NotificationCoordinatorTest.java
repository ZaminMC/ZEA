package net.zaminmc.zea.notify;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coordinator policy, per docs/TESTING.md: overlapping PM + mention,
 * multiple mentions, persistent PM vs temporary mention, priority
 * replacement, expiration, disconnect clearing.
 */
class NotificationCoordinatorTest {
	private static final long NOW = 1_000_000;

	private final RecordingSurface surface = new RecordingSurface();
	private final NotificationCoordinator coordinator = new NotificationCoordinator(surface);

	@Test
	void showsFirstNotificationImmediately() {
		coordinator.post(mention("hey"), NOW);

		assertEquals(List.of("hey"), surface.actionBars);
		assertEquals("hey", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void equalPriorityQueues() {
		coordinator.post(mention("one"), NOW);
		coordinator.post(mention("two"), NOW);

		assertEquals(List.of("one"), surface.actionBars);
		assertEquals(1, coordinator.queued());
	}

	@Test
	void queueCapacityDropsOldestFirst() {
		coordinator.post(mention("first"), NOW);
		coordinator.post(mention("a"), NOW);
		coordinator.post(mention("b"), NOW);
		coordinator.post(mention("c"), NOW);
		coordinator.post(mention("d"), NOW);
		coordinator.post(mention("e"), NOW);

		assertEquals(4, coordinator.queued());
		// "a" was dropped when "e" arrived, not the newest entry.
		assertEquals(List.of("b", "c", "d", "e"), queueTexts());
	}

	@Test
	void privateMessagePreemptsShowingMention() {
		coordinator.post(mention("hello?"), NOW);
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW + 10);

		assertEquals(List.of("hello?", "psst"), surface.actionBars);
		assertEquals("psst", coordinator.showing().orElseThrow().text().getString());
		assertEquals("Coriolanus", coordinator.showing().orElseThrow().key());
	}

	@Test
	void mentionDoesNotPreemptPrivateMessage() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);
		coordinator.post(mention("hello?"), NOW + 10);

		assertEquals(List.of("psst"), surface.actionBars);
		assertEquals(1, coordinator.queued());
	}

	@Test
	void secondPrivateMessageFromSameSenderReplacesInPlace() {
		coordinator.post(privateMessage("Coriolanus", "first"), NOW);
		coordinator.post(privateMessage("Coriolanus", "second"), NOW + 10);

		assertEquals(List.of("first", "second"), surface.actionBars);
		assertEquals(0, coordinator.queued());
		assertEquals("second", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void secondPrivateMessageFromOtherSenderQueues() {
		coordinator.post(privateMessage("Coriolanus", "first"), NOW);
		coordinator.post(privateMessage("pengpy", "second"), NOW + 10);

		assertEquals(List.of("first"), surface.actionBars);
		assertEquals(1, coordinator.queued());
	}

	@Test
	void temporaryNotificationExpiresAndPromotesQueue() {
		coordinator.post(mention("one"), NOW);
		coordinator.post(mention("two"), NOW);

		coordinator.tick(NOW + 4_000);

		assertEquals(List.of("one", "two"), surface.actionBars);
		assertEquals("two", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void persistentPrivateMessageSurvivesPastTemporaryDuration() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);

		coordinator.tick(NOW + 4_000);

		assertEquals("psst", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void persistentPrivateMessageRefreshesActionBar() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);

		// Inside the refresh window: no re-show.
		coordinator.tick(NOW + 2_000);
		assertEquals(List.of("psst"), surface.actionBars);

		// At the window: re-shown so vanilla's ~3 s fade never wins.
		coordinator.tick(NOW + 2_600);
		assertEquals(List.of("psst", "psst"), surface.actionBars);
	}

	@Test
	void persistentPrivateMessageExpiresAtHardCap() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);

		coordinator.tick(NOW + 299_999);
		assertTrue(coordinator.showing().isPresent());

		coordinator.tick(NOW + 300_000);
		assertTrue(coordinator.showing().isEmpty());
		assertTrue(surface.cleared);
	}

	@Test
	void resolveClearsShowingPrivateMessage() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);

		assertTrue(coordinator.resolve("Coriolanus", NOW + 100));

		assertTrue(coordinator.showing().isEmpty());
		assertTrue(surface.cleared);
	}

	@Test
	void resolveClearsQueuedPrivateMessage() {
		coordinator.post(privateMessage("Coriolanus", "showing"), NOW);
		coordinator.post(privateMessage("pengpy", "queued"), NOW);

		assertTrue(coordinator.resolve("pengpy", NOW + 100));
		assertEquals(0, coordinator.queued());

		// The showing one is untouched by the other key.
		assertEquals("showing", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void resolveUnknownKeyChangesNothing() {
		coordinator.post(privateMessage("Coriolanus", "psst"), NOW);

		assertFalse(coordinator.resolve("someone-else", NOW));

		assertEquals("psst", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void resolvePromotesQueuedNotification() {
		coordinator.post(privateMessage("Coriolanus", "first"), NOW);
		coordinator.post(privateMessage("pengpy", "second"), NOW);

		coordinator.resolve("Coriolanus", NOW + 100);

		assertEquals(List.of("first", "second"), surface.actionBars);
		assertEquals("second", coordinator.showing().orElseThrow().text().getString());
	}

	@Test
	void disconnectClearsEverything() {
		coordinator.post(mention("one"), NOW);
		coordinator.post(mention("two"), NOW);

		coordinator.clear();

		assertTrue(coordinator.showing().isEmpty());
		assertEquals(0, coordinator.queued());
		assertTrue(surface.cleared);
	}

	@Test
	void titleDestinationShowsTitleNotActionBar() {
		coordinator.post(Notification.temporary(NotificationKind.MENTION, text("look"),
				Notification.Destination.TITLE, 3_000), NOW);

		assertEquals(0, surface.actionBars.size());
		assertEquals(1, surface.titles.size());
		assertEquals("look", surface.titles.get(0).title().getString());
		assertNull(surface.titles.get(0).subtitle());
		assertEquals(60, surface.titles.get(0).stayTicks());
	}

	@Test
	void titleStayTimeClampsToMinimum() {
		coordinator.post(Notification.temporary(NotificationKind.MENTION, text("look"),
				Notification.Destination.TITLE, 200), NOW);

		assertEquals(10, surface.titles.get(0).stayTicks());
	}

	private List<String> queueTexts() {
		return coordinator.queuedNotifications().stream()
				.map(notification -> notification.text().getString())
				.toList();
	}

	private Notification mention(String text) {
		return Notification.temporary(NotificationKind.MENTION, text(text),
				Notification.Destination.ACTION_BAR, 4_000);
	}

	private Notification privateMessage(String sender, String text) {
		return Notification.persistent(NotificationKind.PRIVATE_MESSAGE, text(text),
				Notification.Destination.ACTION_BAR, 300_000, sender);
	}

	private static Component text(String text) {
		return Component.literal(text);
	}

	/** Records what the coordinator wrote, instead of touching a HUD. */
	private static final class RecordingSurface implements NotificationSurface {
		private record TitleCall(Component title, Component subtitle, int fadeInTicks,
				int stayTicks, int fadeOutTicks) {
		}

		private final List<String> actionBars = new ArrayList<>();
		private final List<TitleCall> titles = new ArrayList<>();
		private boolean cleared;

		@Override
		public void showActionBar(Component text) {
			actionBars.add(text.getString());
		}

		@Override
		public void showTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks,
				int fadeOutTicks) {
			titles.add(new TitleCall(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks));
		}

		@Override
		public void clear() {
			cleared = true;
		}
	}
}
