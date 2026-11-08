package net.zaminmc.zea.buttons;

import net.zaminmc.zea.persistence.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Loads and saves the inventory buttons through the safe-write protocol:
 * serialize in memory, verify the round-trip, atomically replace. A
 * corrupt file is preserved as {@code *.corrupted} and defaults are used
 * instead.
 *
 * <p>Per-entry policy: a hand-edited file may contain one damaged
 * button. {@link ButtonEntry} tolerates absent fields (and explicit
 * nulls, which Gson rejects on primitive record components), unusable
 * entries are dropped with a warning, and duplicated identities are
 * re-issued — one bad entry never takes the whole set down. Buttons are
 * inert data here: nothing in this class, or anything it calls, can send
 * a command; execution lives behind the click path only.
 *
 * <p>Schema: {@code {"version": 1, "buttons": [...]}}. {@code version}
 * gates future migrations the same way {@code configVersion} does for
 * the main configuration.
 */
public final class InventoryButtonStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(InventoryButtonStore.class);

	/** Bump when the file shape changes; add the matching migration below. */
	public static final int CURRENT_VERSION = 1;

	private final Path file;
	private final JsonStore json = new JsonStore();

	public InventoryButtonStore(Path file) {
		this.file = file;
	}

	/** The file shape on disk. */
	private record ButtonFile(int version, List<ButtonEntry> buttons) {
		ButtonFile {
			buttons = buttons == null ? List.of() : buttons;
		}
	}

	/**
	 * One entry as it appears in the file. Every field is optional:
	 * absent or null values become the model's defaults, so a partial
	 * entry degrades into an incomplete button (dropped with a warning
	 * if unusable) instead of a parse failure that would quarantine the
	 * whole file.
	 */
	private record ButtonEntry(
			UUID id,
			String name,
			String command,
			String icon,
			ButtonAnchor anchor,
			Integer offsetX,
			Integer offsetY,
			Boolean enabled) {
		InventoryButton toButton() {
			return new InventoryButton(
					id,
					name,
					command,
					icon,
					anchor,
					offsetX == null ? InventoryButton.DEFAULT_OFFSET_X : offsetX,
					offsetY == null ? InventoryButton.DEFAULT_OFFSET_Y : offsetY,
					enabled == null || enabled);
		}

		static ButtonEntry from(InventoryButton button) {
			return new ButtonEntry(
					button.id(),
					button.name(),
					button.command(),
					button.icon(),
					button.anchor(),
					button.offsetX(),
					button.offsetY(),
					button.enabled());
		}
	}

	/** Loads the button set; missing or corrupt files yield an empty set. */
	public List<InventoryButton> load() {
		ButtonFile data = json.load(file, ButtonFile.class, () -> new ButtonFile(CURRENT_VERSION, List.of()));
		if (data.version() < 1) {
			LOGGER.warn("Buttons file carries no version; treating it as version 1");
		} else if (data.version() < CURRENT_VERSION) {
			LOGGER.info("Migrating inventory buttons from version {}", data.version());
			// switch (data.version()) { case 1: ... } — added when needed.
		}
		return sanitized(data.buttons().stream().map(ButtonEntry::toButton).toList());
	}

	/** Saves the button set after sanitizing every entry. */
	public void save(List<InventoryButton> buttons) {
		List<InventoryButton> clean = sanitized(buttons == null ? List.of() : buttons);
		List<ButtonEntry> entries = clean.stream().map(ButtonEntry::from).toList();
		try {
			json.save(file, new ButtonFile(CURRENT_VERSION, entries));
		} catch (IOException e) {
			LOGGER.warn("Could not save inventory buttons {}", file, e);
		}
	}

	/**
	 * Normalizes entries, re-issues duplicated identities and drops
	 * unusable ones with a warning. The result is safe to render and
	 * hand to the click path.
	 */
	private static List<InventoryButton> sanitized(List<InventoryButton> buttons) {
		List<InventoryButton> clean = new ArrayList<>(buttons.size());
		Set<UUID> seenIds = new HashSet<>();
		for (InventoryButton button : buttons) {
			InventoryButton normalized = button;
			if (!seenIds.add(normalized.id())) {
				LOGGER.warn("Inventory button '{}' reuses an id; issuing a new one", normalized.name());
				normalized = normalized.withNewId();
				seenIds.add(normalized.id());
			}
			if (!normalized.isUsable()) {
				LOGGER.warn("Dropping unusable inventory button '{}' (command or icon missing)", normalized.name());
				continue;
			}
			clean.add(normalized);
		}
		return List.copyOf(clean);
	}
}
