package net.zaminmc.zea.share;

/**
 * A share category: what a player can export and import. Each type owns
 * its payload schema version, so the envelope's {@code version} and the
 * codec evolve together in one place.
 *
 * <p>Two types exist in Phase 1 — inventory buttons and general chat
 * configuration. New types are added only when a real category of
 * shareable configuration warrants one.
 */
public enum ShareType {
        /** The inventory-button set: buttons, icons, positions, commands. */
        INVENTORY_BUTTONS("inventory-buttons", "inven", 1),

        /** Shareable general configuration; never local data or secrets. */
        GENERAL("general", "gen", 1);

        private final String id;
        private final String prefix;
        private final int currentVersion;

        ShareType(String id, String prefix, int currentVersion) {
                this.id = id;
                this.prefix = prefix;
                this.currentVersion = currentVersion;
        }

        /** The {@code type} field of the share envelope. */
        public String id() {
                return id;
        }

        /** The short prefix players type in {@code /zea share <prefix>}. */
        public String prefix() {
                return prefix;
        }

        /** The payload schema version this build reads and writes. */
        public int currentVersion() {
                return currentVersion;
        }

        /**
         * Looks a type up by envelope id or player prefix, case-insensitively.
         *
         * @throws ShareException naming the valid choices when nothing matches
         */
        public static ShareType from(String idOrPrefix) throws ShareException {
                String wanted = idOrPrefix == null ? "" : idOrPrefix.strip().toLowerCase(java.util.Locale.ROOT);
                for (ShareType type : values()) {
                        if (type.id.equals(wanted) || type.prefix.equals(wanted)) {
                                return type;
                        }
                }
                throw new ShareException("Unknown share type '" + idOrPrefix + "'. Valid types: inven, gen");
        }
}
