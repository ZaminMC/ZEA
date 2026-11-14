package net.zaminmc.zea.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.zaminmc.zea.ZeaClient;

/**
 * ModMenu's way into ZEA's settings: the config button on ZEA's entry in
 * the mod list opens the same MoulConfig editor {@code /zea} does. The
 * class loads only when ModMenu is installed — the entrypoint simply never
 * fires without it — so ZEA carries no ModMenu dependency at runtime.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> ConfigScreen.create(ZeaClient.config(),
				parent != null ? parent : Minecraft.getInstance().gui.screen());
	}
}
