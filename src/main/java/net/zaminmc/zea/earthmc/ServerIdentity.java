package net.zaminmc.zea.earthmc;

import java.util.Locale;
import java.util.Optional;

/**
 * The stable identity of the server the client is connected to, used to
 * isolate per-server data (chat history) and nothing else.
 *
 * <p>The key is the <b>hostname the player connected through</b> — the
 * server-list / direct-connect address, before DNS or SRV resolution —
 * normalized to lowercase without port or trailing FQDN dot. That choice
 * is deliberate:
 *
 * <ul>
 * <li>the displayed server name is player-editable and never identity;
 * <li>the resolved socket address changes with hosting moves and SRV
 *     targets, while the connect hostname is what the community
 *     bookmarks and advertises;
 * <li>distinct subdomains stay distinct identities, so separate gates of
 *     the same network keep separate histories.
 * </ul>
 *
 * <p>Identity is only <i>derived</i> for EarthMC connections (ZEA's
 * per-server features are EarthMC-gated), but the normalization itself
 * is generic and side-effect free.
 */
public record ServerIdentity(String host) {
	public ServerIdentity {
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("server identity host must not be blank");
		}
	}

	/**
	 * Derives the identity from a connect address. Empty when the address
	 * carries no usable host (null, blank).
	 */
	public static Optional<ServerIdentity> of(String address) {
		String host = normalizeHost(address);
		return host.isBlank() ? Optional.empty() : Optional.of(new ServerIdentity(host));
	}

	/**
	 * Normalizes a connect address to its host: trimmed, lowercased,
	 * trailing FQDN dot and {@code :port} removed. A trailing port is
	 * stripped by the last colon — server-list entries are
	 * {@code host[:port]} — so bare IPv6 literals are out of scope here
	 * (they never reach per-server data, which is EarthMC-gated and
	 * EarthMC is reached by name).
	 */
	public static String normalizeHost(String address) {
		if (address == null) {
			return "";
		}
		String host = address.trim().toLowerCase(Locale.ROOT);
		if (host.endsWith(".")) {
			host = host.substring(0, host.length() - 1);
		}
		int colon = host.lastIndexOf(':');
		if (colon != -1) {
			host = host.substring(0, colon);
		}
		return host;
	}

	/**
	 * A file-name-safe form of the host for the per-server storage
	 * layout. Normalized hostnames are already {@code [a-z0-9.-]} — this
	 * only defends against the exotic (a hand-crafted server-list entry),
	 * replacing anything else with {@code _}.
	 */
	public String fileSafeName() {
		return host.replaceAll("[^a-z0-9.-]", "_");
	}
}
