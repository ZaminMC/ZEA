package net.zaminmc.zea.buttons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates the inventory-button subsystem: it owns the loaded button
 * set, the edit-mode state, answers hit tests for the container-screen
 * integration, and forwards clicks to the activator — the only path
 * that can send a command. Edits (drag repositioning, the edit screen)
 * flow back through {@link #upsert} and {@link #delete}, which persist
 * through the safe-write store.
 *
 * <p>All state lives on the client thread — rendering and input both
 * arrive there. The store file is the only persistence.
 */
public final class InventoryButtons {
	private final InventoryButtonStore store;
	private final ButtonActivator activator;
	private final ButtonDragSession drag = new ButtonDragSession();
	private List<InventoryButton> buttons = List.of();
	private boolean editMode;

	public InventoryButtons(Path storeFile, ButtonActivator activator) {
		this.store = new InventoryButtonStore(storeFile);
		this.activator = activator;
		this.buttons = List.copyOf(store.load());
	}

	/** The loaded buttons, in draw order. */
	public List<InventoryButton> buttons() {
		return buttons;
	}

	/**
	 * The enabled button under the cursor, if any. Overlapping buttons
	 * resolve to the one drawn last (on top).
	 */
	public InventoryButton buttonAt(int left, int top, int width, int height, double mx, double my) {
		return buttonAt(left, top, width, height, mx, my, false);
	}

	/** The button under the cursor including disabled ones, for edit mode. */
	public InventoryButton editableButtonAt(int left, int top, int width, int height, double mx, double my) {
		return buttonAt(left, top, width, height, mx, my, true);
	}

	private InventoryButton buttonAt(int left, int top, int width, int height, double mx, double my,
			boolean includeDisabled) {
		InventoryButton hit = null;
		for (InventoryButton button : buttons) {
			if ((button.enabled() || includeDisabled)
					&& ButtonGeometry.contains(button, left, top, width, height, mx, my)) {
				hit = button;
			}
		}
		return hit;
	}

	/** Runs the clicked button; see {@link ButtonActivator#activate}. */
	public ButtonActivator.Result click(InventoryButton button, long nowMillis) {
		return activator.activate(button, nowMillis);
	}

	/** Forgets click cooldowns (on disconnect). */
	public void reset() {
		activator.reset();
	}

	/** Whether button editing is active in container screens. */
	public boolean editMode() {
		return editMode;
	}

	/** Toggles button editing; leaving edit mode aborts any drag. */
	public void toggleEditMode() {
		editMode = !editMode;
		if (!editMode) {
			drag.cancel();
		}
	}

	/** The drag session of the current edit mode. */
	public ButtonDragSession drag() {
		return drag;
	}

	/** Inserts or replaces a button by identity, then persists. */
	public void upsert(InventoryButton updated) {
		if (updated == null) {
			throw new NullPointerException("updated must not be null");
		}
		List<InventoryButton> next = new ArrayList<>(buttons);
		boolean replaced = false;
		for (int i = 0; i < next.size(); i++) {
			if (next.get(i).id().equals(updated.id())) {
				next.set(i, updated);
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			next.add(updated);
		}
		buttons = List.copyOf(next);
		store.save(buttons);
	}

	/** Removes a button by identity, then persists. */
	public void delete(UUID id) {
		if (id == null) {
			throw new NullPointerException("id must not be null");
		}
		List<InventoryButton> next = new ArrayList<>(buttons);
		next.removeIf(button -> button.id().equals(id));
		buttons = List.copyOf(next);
		store.save(buttons);
	}

	/**
	 * Replaces the whole set with an imported one, then persists.
	 * The import path validates strictly beforehand; nothing here
	 * executes a command — buttons stay inert data until clicked.
	 */
	public void applyImport(List<InventoryButton> imported) {
		buttons = List.copyOf(imported == null ? List.of() : imported);
		store.save(buttons);
	}
}
