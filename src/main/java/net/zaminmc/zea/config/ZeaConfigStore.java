package net.zaminmc.zea.config;

import net.zaminmc.zea.persistence.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads and saves {@link ZeaConfig} through the safe-write protocol: the
 * file is serialized and verified in memory, then atomically replaced. A
 * corrupt file is preserved as {@code *.corrupted} and defaults are used
 * instead — the editor can never crash on a damaged config, and a failed
 * save never partially overwrites it.
 */
public final class ZeaConfigStore {
        private static final Logger LOGGER = LoggerFactory.getLogger(ZeaConfigStore.class);

        private final Path file;
        private final JsonStore json = new JsonStore();

        public ZeaConfigStore(Path file) {
                this.file = file;
        }

        /** Loads the configuration, applying migrations for older schema versions. */
        public ZeaConfig load() {
                ZeaConfig config = json.load(file, ZeaConfig.class, ZeaConfig::new);
                if (config.configVersion() < ZeaConfig.CURRENT_VERSION) {
                        migrate(config);
                        config.markMigrated();
                        save(config);
                }
                config.setSaveAction(() -> save(config));
                return config;
        }

        /**
         * Saves the configuration. Failures are logged, not thrown — the MoulConfig
         * editor calls {@link ZeaConfig#saveNow()} from its own UI callbacks.
         */
        public void save(ZeaConfig config) {
                try {
                        json.save(file, config);
                } catch (IOException e) {
                        LOGGER.warn("Could not save configuration {}", file, e);
                }
        }

        /**
         * Structured migrations, one case per source version. Version 1 is the
         * first schema, so there is nothing to migrate yet.
         */
        private static void migrate(ZeaConfig config) {
                LOGGER.info("Migrating configuration from version {}", config.configVersion());
                // switch (config.configVersion()) { case 1: ... } — added when needed.
        }
}
