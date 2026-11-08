package net.zaminmc.zea;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.zaminmc.zea.buttons.ButtonActivator;
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
                                FabricLoader.getInstance().getConfigDir().resolve("zea/buttons.json"),
                                new ButtonActivator(zea$commandSender(), ButtonActivator.DEFAULT_COOLDOWN_MILLIS));
                ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> inventoryButtons.reset());
                ZeaKeys.register();
        }

        /** Sends a button's command through the play connection. */
        private static ButtonActivator.CommandSender zea$commandSender() {
                return command -> {
                        ClientPacketListener connection = Minecraft.getInstance().getConnection();
                        if (connection == null) {
                                throw new IllegalStateException("not connected to a server");
                        }
                        connection.sendCommand(command);
                };
        }
}
