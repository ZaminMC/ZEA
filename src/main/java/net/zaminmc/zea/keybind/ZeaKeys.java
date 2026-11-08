package net.zaminmc.zea.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * ZEA's key bindings, registered into the vanilla controls screen.
 *
 * <p>ZEA's built-in shortcuts are modifier-based mouse and keyboard
 * combinations (right-click copy, alt + right-click formatted copy,
 * Ctrl+F search) that the vanilla key-mapping system cannot express —
 * a binding is one key without modifiers. These bindings therefore
 * offer *additional*, rebindable triggers for screen-scoped actions;
 * the built-in shortcuts always keep working.
 */
public final class ZeaKeys {
        /** ZEA's group in the controls screen. */
        private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
                        Identifier.fromNamespaceAndPath("zea", "zea"));

        /**
         * Toggles the chat search field while the chat screen is open.
         * Unbound by default — Ctrl+F (Cmd+F on macOS) already does this.
         */
        public static final KeyMapping SEARCH_CHAT = new KeyMapping(
                        "zea.key.search_chat", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

        private ZeaKeys() {
        }

        /** Registers the bindings; called once from the client entrypoint. */
        public static void register() {
                KeyMappingHelper.registerKeyMapping(SEARCH_CHAT);
        }
}
