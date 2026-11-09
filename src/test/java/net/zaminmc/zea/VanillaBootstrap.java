package net.zaminmc.zea;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Boots vanilla's static state for unit tests. The game client does this
 * at startup; a bare test JVM must do it before anything touches the
 * registries — the component codec, for instance, pulls in click-event
 * actions whose static initializers read {@code BuiltInRegistries}.
 *
 * <p>Idempotent and cheap after the first call; tests that never leave
 * plain component construction never need it.
 */
public final class VanillaBootstrap {
	private static boolean bootstrapped;

	private VanillaBootstrap() {
	}

	public static synchronized void ensure() {
		if (bootstrapped) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		bootstrapped = true;
	}
}
