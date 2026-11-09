package net.zaminmc.zea.notify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.PrivateMessage;
import net.zaminmc.zea.config.ZeaConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PM flow through the notifications service: incoming PMs post
 * persistent notifications, replies resolve them, and configuration gates
 * both the notice and the sound.
 */
class NotificationsTest {
	private static final long NOW = 1_000_000;

	private final RecordingSurface surface = new RecordingSurface();
	private final RecordingSound sound = new RecordingSound();
	private final ZeaConfig config = new ZeaConfig();
	private final Notifications notifications = new Notifications(surface, config, sound);

	@Test
	void incomingPrivateMessagePostsPersistentNotification() {
		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(1, surface.actionBars.size());
		assertTrue(surface.actionBars.get(0).startsWith("PM from Coriolanus: psst"));
		assertEquals("Coriolanus", notifications.coordinator().showing().orElseThrow().key());
	}

	@Test
	void notificationTextIsStyledAndCarriesTheContent() {
		notifications.onClassified(pm("Coriolanus", "hello there", PrivateMessage.Direction.INCOMING), NOW);

		String text = notifications.coordinator().showing().orElseThrow().text().getString();
		assertEquals("PM from Coriolanus: hello there", text);
	}

	@Test
	void longContentIsTruncatedForTheActionBar() {
		notifications.onClassified(pm("Coriolanus", "x".repeat(200), PrivateMessage.Direction.INCOMING), NOW);

		String text = notifications.coordinator().showing().orElseThrow().text().getString();
		assertTrue(text.length() <= "PM from Coriolanus: ".length() + 60, "preview is bounded");
		assertTrue(text.endsWith("..."));
	}

	@Test
	void incomingPrivateMessagePlaysCue() {
		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(List.of(NotificationKind.PRIVATE_MESSAGE), sound.played);
	}

	@Test
	void replyResolvesThePrivateMessageNotification() {
		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		notifications.onClassified(pm("Coriolanus", "hi", PrivateMessage.Direction.OUTGOING), NOW + 100);

		assertTrue(notifications.coordinator().showing().isEmpty());
		assertTrue(surface.cleared);
	}

	@Test
	void replyToSomeoneElseKeepsTheNotification() {
		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		notifications.onClassified(pm("pengpy", "hi", PrivateMessage.Direction.OUTGOING), NOW + 100);

		assertEquals("Coriolanus", notifications.coordinator().showing().orElseThrow().key());
	}

	@Test
	void disabledNotificationsShowNothingButStillResolve() {
		config.notifications.pmNotifications = false;

		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(0, surface.actionBars.size());
		assertEquals(0, sound.played.size());

		// The reply path stays active: no stale notice can linger.
		notifications.onClassified(pm("Coriolanus", "hi", PrivateMessage.Direction.OUTGOING), NOW + 100);
	}

	@Test
	void disabledSoundStaysSilent() {
		config.notifications.pmSound = false;

		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(1, surface.actionBars.size());
		assertEquals(0, sound.played.size());
	}

	@Test
	void titleDestinationIsConfigurable() {
		config.notifications.pmDestination = Notification.Destination.TITLE;

		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(0, surface.actionBars.size());
		assertEquals(1, surface.titles.size());
	}

	@Test
	void unsetDestinationFallsBackToActionBar() {
		config.notifications.pmDestination = null;

		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		assertEquals(1, surface.actionBars.size());
	}

	@Test
	void nonPrivateMessagesAreIgnored() {
		notifications.onClassified(ClassifiedMessage.unclassified(
				CapturedMessage.of(Component.literal("[Town] Someone: hi"), Instant.EPOCH)), NOW);

		assertEquals(0, surface.actionBars.size());
		assertEquals(0, sound.played.size());
	}

	@Test
	void disconnectClears() {
		notifications.onClassified(pm("Coriolanus", "psst", PrivateMessage.Direction.INCOMING), NOW);

		notifications.clear();

		assertTrue(notifications.coordinator().showing().isEmpty());
	}

	private ClassifiedMessage pm(String other, String content, PrivateMessage.Direction direction) {
		PrivateMessage payload = switch (direction) {
			case INCOMING -> new PrivateMessage(other, "me", direction, content);
			case OUTGOING -> new PrivateMessage("me", other, direction, content);
		};
		return new ClassifiedMessage(
				CapturedMessage.of(Component.literal("[" + other + " → me] " + content), Instant.EPOCH),
				MessageCategory.PRIVATE_MESSAGE, payload);
	}

	private static final class RecordingSurface implements NotificationSurface {
		private record TitleCall(String title, String subtitle) {
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
			titles.add(new TitleCall(title.getString(), subtitle == null ? null : subtitle.getString()));
		}

		@Override
		public void clear() {
			cleared = true;
		}
	}

	private static final class RecordingSound implements NotificationSound {
		private final List<NotificationKind> played = new ArrayList<>();

		@Override
		public void play(NotificationKind kind) {
			played.add(kind);
		}
	}
}
