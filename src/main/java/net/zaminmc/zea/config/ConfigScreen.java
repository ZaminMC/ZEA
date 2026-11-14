package net.zaminmc.zea.config;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Opens the MoulConfig editor for {@link ZeaConfig} — the {@code /zea}
 * menu and the ModMenu button. The editor writes fields through
 * reflection and saves via {@link ZeaConfig#saveNow()}, which the store
 * wires to the safe-write protocol, so a failed save never damages the
 * file on disk.
 *
 * <p>The processor needs two steps the editor does not do itself (the
 * dev.24 live defect: {@code /zea} showed nothing because both were
 * missing — the editor rendered zero categories over an unprocessed
 * config). {@link MoulConfigProcessor#withDefaults} registers the editor
 * factories for the annotations {@link ZeaConfig} uses; without it every
 * option is dropped with a warning. {@link ConfigProcessorDriver} then
 * walks the annotated fields and finalizes the processor; without it
 * {@code getAllCategories()} is still empty and the finalized check
 * warns. Both verified against the bundled 4.8.0 sources.
 *
 * <p>The {@code @Expose} convention the driver checks by default belongs
 * to MoulConfig's own Gson persistence; ZEA persists through its own
 * store, so {@code checkExpose} is switched off instead of annotating
 * fields for a serializer they never meet.
 *
 * <p>Works anywhere — configuration is not EarthMC-specific. Closing the
 * editor returns to whatever screen the player opened it from.
 */
public final class ConfigScreen {
	private ConfigScreen() {
	}

	/** Processes the config into the shape the editor renders. */
	public static MoulConfigProcessor<ZeaConfig> process(ZeaConfig config) {
		MoulConfigProcessor<ZeaConfig> processor = MoulConfigProcessor.withDefaults(config);
		ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);
		driver.checkExpose = false;
		driver.processConfig(config);
		return processor;
	}

	/** Builds the editor screen; {@code previous} is restored on close. */
	public static Screen create(ZeaConfig config, Screen previous) {
		MoulConfigEditor<ZeaConfig> editor = new MoulConfigEditor<>(process(config));
		return new MoulConfigScreenComponent(
				Component.literal("ZEA"),
				new GuiContext(new GuiElementComponent(editor)),
				previous);
	}

	/** Builds and shows the editor; call on the client thread. */
	public static void open(Minecraft minecraft, ZeaConfig config) {
		minecraft.gui.setScreen(create(config, minecraft.gui.screen()));
	}
}
