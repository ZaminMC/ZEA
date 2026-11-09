package net.zaminmc.zea.share;

import java.net.URI;
import java.util.Locale;

/**
 * A HelpChat paste key, parsed from whatever the player pasted: a bare
 * key ({@code wozajojele}), a presentation URL
 * ({@code https://paste.helpcha.at/wozajojele.java}) or a raw URL
 * ({@code https://paste.helpcha.at/raw/wozajojele}).
 *
 * <p>Accepting exactly one HTTPS host and a whitelist key charset is the
 * import path's URL gate: other hosts, other schemes, queries,
 * fragments and traversal-shaped keys are all rejected here, before any
 * URL is built. The key charset is {@code [A-Za-z0-9_-]} — observed keys
 * are alphanumeric; the two extra symbols stay harmless while keeping
 * every delimiter and escape character out.
 *
 * @param key the bare paste key
 */
public record PasteKey(String key) {
        /** The only host imports may come from. */
        public static final String HOST = "paste.helpcha.at";

        /** The production service root; HTTPS is the only allowed scheme. */
        public static final String BASE_URL = "https://" + HOST;

        /** Key length bounds; a service key is never anywhere near 64. */
        private static final int MAX_KEY_LENGTH = 64;

        private static final String KEY_PATTERN = "[A-Za-z0-9_-]+";

        public PasteKey {
                if (!isValidKey(key)) {
                        throw new IllegalArgumentException("not a valid paste key: " + key);
                }
        }

        /** The presentation URL shown to players. */
        public String url() {
                return BASE_URL + "/" + key;
        }

        @Override
        public String toString() {
                return key;
        }

        /** Whether a bare key matches the whitelist charset and bounds. */
        public static boolean isValidKey(String key) {
                return key != null && !key.isEmpty() && key.length() <= MAX_KEY_LENGTH
                                && key.matches(KEY_PATTERN);
        }

        /**
         * Parses player input into a key.
         *
         * @throws ShareException with a player-readable reason when the
         *                         input is not a HelpChat paste reference
         */
        public static PasteKey parse(String input) throws ShareException {
                String trimmed = input == null ? "" : input.strip();
                if (trimmed.isEmpty()) {
                        throw new ShareException("Give a paste key or paste URL, e.g. wozajojele "
                                        + "or https://" + HOST + "/wozajojele");
                }
                if (trimmed.contains("://") || trimmed.startsWith("//")) {
                        return fromUrl(URI.create(trimmed));
                }
                if (trimmed.contains("/") || trimmed.contains(":")) {
                        throw new ShareException("That does not look like a paste key or a "
                                        + "https://" + HOST + "/ URL");
                }
                return new PasteKey(stripExtension(trimmed));
        }

        private static PasteKey fromUrl(URI url) throws ShareException {
                String scheme = url.getScheme() == null ? "" : url.getScheme().toLowerCase(Locale.ROOT);
                if (!"https".equals(scheme)) {
                        throw new ShareException("Paste URLs must be HTTPS");
                }
                String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
                if (!HOST.equals(host)) {
                        throw new ShareException("Only paste URLs from " + HOST + " can be imported");
                }
                if (url.getRawQuery() != null || url.getRawFragment() != null) {
                        throw new ShareException("That paste URL has extra parts ZEA does not accept");
                }
                String path = url.getRawPath() == null ? "" : url.getRawPath();
                String[] segments = path.split("/");
                String candidate = null;
                if (segments.length == 2) {
                        candidate = segments[1]; // /{key}
                } else if (segments.length == 3 && "raw".equals(segments[1])) {
                        candidate = segments[2]; // /raw/{key}
                }
                if (candidate == null || candidate.isEmpty()) {
                        throw new ShareException("That paste URL has no key in it");
                }
                return new PasteKey(stripExtension(candidate));
        }

        /**
         * Drops a presentation-only extension such as {@code .java} or
         * {@code .json} — several suffixes exist and none change the key.
         */
        private static String stripExtension(String key) throws ShareException {
                String bare = key.contains(".")
                                ? key.substring(0, key.indexOf('.'))
                                : key;
                if (!isValidKey(bare)) {
                        throw new ShareException("'" + key + "' is not a valid paste key");
                }
                return bare;
        }
}
