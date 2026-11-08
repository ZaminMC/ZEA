package net.zaminmc.zea.buttons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * The tooltip lines shown when hovering a button. Pure text assembly:
 * icon resolution is the caller's concern ({@link ButtonIcons}), passed
 * in as a flag so this class stays testable without registries.
 */
public final class ButtonTooltips {
	private ButtonTooltips() {
	}

	/**
	 * Lines for a hovered button: its name, the command it would run,
	 * and a warning when the configured icon does not resolve.
	 *
	 * @param iconKnown whether {@link ButtonIcons} resolved the icon id
	 */
	public static List<Component> lines(InventoryButton button, boolean iconKnown) {
		List<Component> lines = new ArrayList<>(3);
		lines.add(Component.literal(button.name()));
		MutableComponent command = Component.literal("/" + button.command());
		lines.add(command.withStyle(ChatFormatting.GRAY));
		if (!iconKnown) {
			MutableComponent unknown = Component.translatable("zea.buttons.unknown_icon", button.icon());
			lines.add(unknown.withStyle(ChatFormatting.RED));
		}
		return lines;
	}
}
