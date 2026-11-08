package net.zaminmc.zea;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.zaminmc.zea.buttons.InventoryButtons;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.config.ZeaConfigStore;
import net.zaminmc.zea.keybind.ZeaKeys;
import net.zaminmc.zea.lifecycle.ClientLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * ZEA entrypoint. Performs wiring only; subsystem behavior lives in the
 * packages described in ARCHITECTURE.md.
 */
public final class ZeaClient implements ClientModInitializer {
        public static final String MOD_ID = "zea";
        public static final Logger LOGGER = LoggerFactory.getLogger("ZEA");

        private static ClientLifecycle lifecycle;
        private static ChatSubsystem chat;
        private static InventoryButtons inventoryButtons;
        private static ZeaConfig config;

        public static ClientLifecycle lifecycle() {
                return lifecycle;
        }

        public static ChatSubsystem chat() {
                return chat;
        }

        public static InventoryButtons inventoryButtons() {
                return inventoryButtons;
        }

        public static ZeaConfig config() {
                return config;
        }

        @Override
        public void onInitializeClient() {
                LOGGER.info("ZEA loading");
                Path configFile = FabricLoader.getInstance().getConfigDir().resolve("zea.json");
                config = new ZeaConfigStore(configFile).load();
                lifecycle = new ClientLifecycle();
                chat = new ChatSubsystem(lifecycle, new ClassificationPipeline(), config);
                inventoryButtons = new InventoryButtons(
                                FabricLoader.getInstance().getConfigDir().resolve("zea/buttons.json"));
                ZeaKeys.register();
        }
}
