package net.zaminmc.zea.buttons;

import java.nio.file.Path;
import java.util.List;

/**
 * Coordinates the inventory-button subsystem: it owns the loaded button
 * set, answers hit tests for the container-screen integration, and
 * forwards clicks to the activator — the only path that can send a
 * command.
 *
 * <p>All state lives on the client thread — rendering and input both
 * arrive there. The store file is the only persistence.
 */
public final class InventoryButtons {
        private final InventoryButtonStore store;
        private final ButtonActivator activator;
        private List<InventoryButton> buttons = List.of();

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
         * The button under the cursor, if any. Overlapping buttons resolve
         * to the one drawn last (on top).
         */
        public InventoryButton buttonAt(int left, int top, int width, int height, double mx, double my) {
                InventoryButton hit = null;
                for (InventoryButton button : buttons) {
                        if (button.enabled()
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
}
