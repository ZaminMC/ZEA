package net.zaminmc.zea.buttons;

import java.util.UUID;

/**
 * One configurable inventory button: an item icon beside the container
 * GUI that runs a chat command when clicked.
 *
 * <p>The record tolerates partial data (null or malformed fields get safe
 * defaults in {@link #normalized()}) because it is deserialized straight
 * from a user-editable file — a single damaged entry must never crash
 * loading. Whether a button is complete enough to keep is decided by
 * {@link #isUsable()}, applied at the store boundary.
 *
 * @param id      stable identity, used by the editor and share imports
 * @param name    tooltip title; blank normalizes to {@link #DEFAULT_NAME}
 * @param command chat command without the leading slash
 * @param icon    item id of the icon, e.g. {@code minecraft:compass}
 * @param anchor  container corner the offset is measured from
 * @param offsetX horizontal distance from the anchor's flush position
 * @param offsetY vertical distance from the anchor's flush position
 * @param enabled disabled buttons are kept but not rendered or clickable
 */
public record InventoryButton(
		UUID id,
		String name,
		String command,
		String icon,
		ButtonAnchor anchor,
		int offsetX,
		int offsetY,
		boolean enabled) {
	/** Fallback title when a name is missing or blank. */
	public static final String DEFAULT_NAME = "Button";

	/** Longest command the chat packet accepts. */
	public static final int MAX_COMMAND_LENGTH = 256;

	/** Longest name kept; longer names are truncated, not rejected. */
	public static final int MAX_NAME_LENGTH = 64;

	/** Longest item id kept. */
	public static final int MAX_ICON_LENGTH = 128;

	/** Offset bounds; buttons must stay within this distance of the GUI. */
	public static final int MIN_OFFSET = -512;
	public static final int MAX_OFFSET = 512;

	/** The offset of a newly created button: beside the container's right edge. */
	public static final int DEFAULT_OFFSET_X = 22;
	public static final int DEFAULT_OFFSET_Y = 2;

	public InventoryButton {
		id = id == null ? UUID.randomUUID() : id;
		name = cleaned(name, MAX_NAME_LENGTH, DEFAULT_NAME);
		command = withoutLeadingSlash(cleaned(command, MAX_COMMAND_LENGTH, ""));
		icon = cleaned(icon, MAX_ICON_LENGTH, "");
		anchor = anchor == null ? ButtonAnchor.TOP_RIGHT : anchor;
		offsetX = Math.clamp(offsetX, MIN_OFFSET, MAX_OFFSET);
		offsetY = Math.clamp(offsetY, MIN_OFFSET, MAX_OFFSET);
	}

	/**
	 * Creates a starting point for the editor: usable icon and anchor,
	 * empty command so it cannot be saved or run until filled in.
	 */
	public static InventoryButton newButton(int index) {
		return new InventoryButton(
				UUID.randomUUID(),
				DEFAULT_NAME + " " + index,
				"",
				"minecraft:compass",
				ButtonAnchor.TOP_RIGHT,
				DEFAULT_OFFSET_X,
				DEFAULT_OFFSET_Y,
				true);
	}

	/** Whether this button has the data needed to render and run. */
	public boolean isUsable() {
		return validCommand(command) && validIcon(icon);
	}

	/** Whether the current offset is at the clamp bound on either axis. */
	public boolean offsetClamped() {
		return offsetX == MIN_OFFSET || offsetX == MAX_OFFSET
				|| offsetY == MIN_OFFSET || offsetY == MAX_OFFSET;
	}

	/** A copy with this command. */
	public InventoryButton withCommand(String command) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy with this name. */
	public InventoryButton withName(String name) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy with this icon. */
	public InventoryButton withIcon(String icon) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy with this anchor. */
	public InventoryButton withAnchor(ButtonAnchor anchor) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy with this offset. */
	public InventoryButton withOffset(int offsetX, int offsetY) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy with this enabled flag. */
	public InventoryButton withEnabled(boolean enabled) {
		return new InventoryButton(id, name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/** A copy of this button with a fresh identity. */
	public InventoryButton withNewId() {
		return new InventoryButton(UUID.randomUUID(), name, command, icon, anchor, offsetX, offsetY, enabled);
	}

	/**
	 * Whether a command can be sent: non-blank, within the packet limit
	 * and free of control characters (newlines could smuggle extra lines
	 * into one packet).
	 */
	public static boolean validCommand(String command) {
		if (command == null || command.isBlank()) {
			return false;
		}
		if (command.length() > MAX_COMMAND_LENGTH) {
			return false;
		}
		return command.chars().allMatch(c -> !Character.isISOControl(c));
	}

	/** Whether an icon id is well-formed (existing items are checked at render). */
	public static boolean validIcon(String icon) {
		return icon != null && !icon.isBlank() && icon.length() <= MAX_ICON_LENGTH
				&& !icon.chars().anyMatch(Character::isISOControl);
	}

	/**
	 * Trims, strips control characters and truncates. Blank input becomes
	 * {@code fallback}; a name that is entirely control characters falls
	 * back too.
	 */
	private static String cleaned(String value, int maxLength, String fallback) {
		if (value == null) {
			return fallback;
		}
		StringBuilder cleaned = new StringBuilder(value.length());
		value.chars().filter(c -> !Character.isISOControl(c)).limit(maxLength).forEach(cleaned::appendCodePoint);
		String trimmed = cleaned.toString().strip();
		return trimmed.isEmpty() ? fallback : trimmed;
	}

	private static String withoutLeadingSlash(String command) {
		return command.startsWith("/") ? command.substring(1) : command;
	}
}
