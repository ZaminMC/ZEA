package net.zaminmc.zea.buttons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The single path from a button to a sent command. Clicks are validated
 * again here (defense in depth: a hand-edited store, or a future import,
 * must never smuggle a malformed command past the click), and each
 * button has a short cooldown so a double-click sends exactly one
 * command.
 *
 * <p>This class is deliberately the only place that can invoke a
 * {@link CommandSender}: loading, saving and importing buttons never
 * execute them.
 */
public final class ButtonActivator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ButtonActivator.class);

	/** Guards against double-clicks; one command per click, one click per window. */
	public static final long DEFAULT_COOLDOWN_MILLIS = 500;

	/** Sends one command (without the leading slash). Throws when the client is not connected. */
	@FunctionalInterface
	public interface CommandSender {
		void send(String command) throws Exception;
	}

	/** Outcome of a click. */
	public enum Result {
		EXECUTED,
		/** The button was clicked again within its cooldown. */
		COOLDOWN,
		/** The button's data is not valid enough to run. */
		INVALID,
		/** Sending failed (e.g. not connected). */
		FAILED
	}

	private final CommandSender sender;
	private final long cooldownMillis;
	private final Map<UUID, Long> lastExecution = new HashMap<>();

	public ButtonActivator(CommandSender sender, long cooldownMillis) {
		if (sender == null) {
			throw new NullPointerException("sender must not be null");
		}
		if (cooldownMillis < 0) {
			throw new IllegalArgumentException("cooldown must not be negative");
		}
		this.sender = sender;
		this.cooldownMillis = cooldownMillis;
	}

	/** Runs the button's command, subject to validation and cooldown. */
	public Result activate(InventoryButton button, long nowMillis) {
		if (button == null || !button.enabled() || !InventoryButton.validCommand(button.command())) {
			return Result.INVALID;
		}
		Long last = lastExecution.get(button.id());
		if (last != null && nowMillis - last < cooldownMillis) {
			return Result.COOLDOWN;
		}
		try {
			sender.send(button.command());
		} catch (Exception | LinkageError e) {
			LOGGER.warn("Button '{}' could not send /{}: {}", button.name(), button.command(), e.toString());
			return Result.FAILED;
		}
		lastExecution.put(button.id(), nowMillis);
		return Result.EXECUTED;
	}

	/** Clears cooldown memory (e.g. when disconnecting). */
	public void reset() {
		lastExecution.clear();
	}

	/** The cooldown window, in milliseconds. */
	public long cooldownMillis() {
		return cooldownMillis;
	}
}
