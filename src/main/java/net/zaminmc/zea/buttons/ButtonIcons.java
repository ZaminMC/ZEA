package net.zaminmc.zea.buttons;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves button icons to item stacks for rendering. Unknown or
 * malformed ids fall back to the barrier item — the vanilla convention
 * for missing content — instead of hiding the button, so a typo is
 * visible (and flagged in the tooltip) rather than silent.
 *
 * <p>Stacks are cached per id: item icons are needed every frame while a
 * container screen is open, and button sets are small. Registry lookups
 * happen on the client thread during rendering, matching vanilla's own
 * GUI item access.
 */
public final class ButtonIcons {
        /** Rendered when the configured icon id does not resolve to an item. */
        private static final String FALLBACK_ICON = "minecraft:barrier";

        /** Cache cap; button sets are tiny, this only guards pathological input. */
        private static final int MAX_CACHE_SIZE = 64;

        private static final Map<String, ItemStack> CACHE = new ConcurrentHashMap<>();

        private ButtonIcons() {
        }

        /** The stack to draw for an icon id, falling back when it cannot resolve. */
        public static ItemStack iconFor(String iconId) {
                String id = iconId == null ? FALLBACK_ICON : iconId;
                ItemStack cached = CACHE.get(id);
                if (cached != null) {
                        return cached;
                }
                ItemStack stack = resolve(id);
                if (CACHE.size() >= MAX_CACHE_SIZE) {
                        CACHE.clear();
                }
                CACHE.put(id, stack);
                return stack;
        }

        /** Whether an icon id resolves to a registered item. */
        public static boolean isKnown(String iconId) {
                Identifier id = Identifier.tryParse(iconId);
                return id != null && BuiltInRegistries.ITEM.containsKey(id);
        }

        private static ItemStack resolve(String id) {
                Identifier parsed = Identifier.tryParse(id);
                if (parsed != null) {
                        return BuiltInRegistries.ITEM.getOptional(parsed)
                                        .map(ItemStack::new)
                                        .orElseGet(() -> new ItemStack(Items.BARRIER));
                }
                return new ItemStack(Items.BARRIER);
        }
}
