package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonActivatorTest {
	/** Records sends; throws when asked to simulate a dead connection. */
	private static final class RecordingSender implements ButtonActivator.CommandSender {
		final List<String> sent = new ArrayList<>();
		boolean fail;

		@Override
		public void send(String command) throws Exception {
			if (fail) {
				throw new IllegalStateException("not connected");
			}
			sent.add(command);
		}
	}

	private static InventoryButton usable(String command) {
		return InventoryButton.newButton(1).withName("Home").withCommand(command);
	}

	@Test
	void sendsTheCommandOfAClickedButton() {
		RecordingSender sender = new RecordingSender();
		ButtonActivator activator = new ButtonActivator(sender, 500);

		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(usable("town spawn"), 1_000));
		assertEquals(List.of("town spawn"), sender.sent);
	}

	@Test
	void refusesInvalidOrDisabledButtons() {
		RecordingSender sender = new RecordingSender();
		ButtonActivator activator = new ButtonActivator(sender, 500);

		// The model itself sanitizes control characters and length, so the
		// only shapes reaching the activator are blank commands and
		// disabled buttons; it still re-checks validity as defense in depth.
		assertEquals(ButtonActivator.Result.INVALID, activator.activate(usable(""), 1_000));
		assertEquals(ButtonActivator.Result.INVALID, activator.activate(usable("spawn").withEnabled(false), 1_000));
		assertEquals(ButtonActivator.Result.INVALID, activator.activate(null, 1_000));
		assertEquals(List.of(), sender.sent);
	}

	@Test
	void doubleClicksSendOneCommand() {
		RecordingSender sender = new RecordingSender();
		ButtonActivator activator = new ButtonActivator(sender, 500);
		InventoryButton button = usable("town spawn");

		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(button, 1_000));
		assertEquals(ButtonActivator.Result.COOLDOWN, activator.activate(button, 1_400));
		assertEquals(List.of("town spawn"), sender.sent);

		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(button, 1_500));
		assertEquals(2, sender.sent.size(), "after the window a click runs again");
	}

	@Test
	void cooldownsArePerButton() {
		RecordingSender sender = new RecordingSender();
		ButtonActivator activator = new ButtonActivator(sender, 500);

		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(usable("spawn"), 1_000));
		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(usable("home"), 1_000));
		assertEquals(2, sender.sent.size());
	}

	@Test
	void failedSendsReportFailureWithoutArmingTheCooldown() {
		RecordingSender sender = new RecordingSender();
		sender.fail = true;
		ButtonActivator activator = new ButtonActivator(sender, 500);
		InventoryButton button = usable("town spawn");

		assertEquals(ButtonActivator.Result.FAILED, activator.activate(button, 1_000));

		sender.fail = false;
		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(button, 1_100),
				"a failed attempt should not lock the button");
	}

	@Test
	void resetForgetsCooldowns() {
		RecordingSender sender = new RecordingSender();
		ButtonActivator activator = new ButtonActivator(sender, 500);
		InventoryButton button = usable("town spawn");

		activator.activate(button, 1_000);
		activator.reset();

		assertEquals(ButtonActivator.Result.EXECUTED, activator.activate(button, 1_100));
	}
}
