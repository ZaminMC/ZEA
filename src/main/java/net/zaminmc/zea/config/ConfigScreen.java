package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Opens the MoulConfig editor for {@link ZeaConfig} — the {@code /zea}
 * menu. The editor writes fields through reflection and saves via
 * {@link ZeaConfig#saveNow()}, which the store wires to the safe-write
 * protocol, so a failed save never damages the file on disk.
 *
 * <p>Works anywhere — configuration is not EarthMC-specific. Closing the
 * editor returns to whatever screen the player opened it from.
 */
public final class ConfigScreen {
	private ConfigScreen() {
	}

	/** Builds and shows the editor; call on the client thread. */
	public static void open(Minecraft minecraft, ZeaConfig config) {
		MoulConfigProcessor<ZeaConfig> processor = new MoulConfigProcessor<>(config);
		MoulConfigEditor<ZeaConfig> editor = new MoulConfigEditor<>(processor);
		MoulConfigScreenComponent screen = new MoulConfigScreenComponent(
				Component.literal("ZEA"),
				new GuiContext(new GuiElementComponent(editor)),
				minecraft.gui.screen());
		minecraft.gui.setScreen(screen);
	}
}
