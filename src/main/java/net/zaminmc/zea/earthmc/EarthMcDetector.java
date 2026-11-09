package net.zaminmc.zea.earthmc;

/**
 * Detects whether the client is connected to EarthMC.
 *
 * <p>EarthMC is identified by its server address (exact match or a
 * subdomain of {@code earthmc.net}). The port is ignored. Host
 * normalization is owned by {@link ServerIdentity}.
 */
public final class EarthMcDetector {
	private static final String DOMAIN = "earthmc.net";

	private EarthMcDetector() {
	}

	public static boolean isEarthMcAddress(String address) {
		String host = ServerIdentity.normalizeHost(address);
		return host.equals(DOMAIN) || host.endsWith("." + DOMAIN);
	}
}
