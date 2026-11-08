package net.zaminmc.zea.earthmc;

import java.util.Locale;

/**
 * Detects whether the client is connected to EarthMC.
 *
 * <p>EarthMC is identified by its server address (exact match or a
 * subdomain of {@code earthmc.net}). The port is ignored.
 */
public final class EarthMcDetector {
	private static final String DOMAIN = "earthmc.net";

	private EarthMcDetector() {
	}

	public static boolean isEarthMcAddress(String address) {
		if (address == null) {
			return false;
		}
		String host = stripPort(address.trim().toLowerCase(Locale.ROOT));
		return host.equals(DOMAIN) || host.endsWith("." + DOMAIN);
	}

	private static String stripPort(String address) {
		int colon = address.lastIndexOf(':');
		if (colon == -1) {
			return address;
		}
		// IPv6 literals in brackets keep their colons; server list entries
		// here are host[:port], so only a trailing ":port" is stripped.
		return address.substring(0, colon);
	}
}
