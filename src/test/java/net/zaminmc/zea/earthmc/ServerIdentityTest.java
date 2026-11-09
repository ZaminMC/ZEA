package net.zaminmc.zea.earthmc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerIdentityTest {
	@ParameterizedTest
	@CsvSource({
			"earthmc.net, earthmc.net",
			"  EarthMC.Net  , earthmc.net",
			"earthmc.net:25565, earthmc.net",
			"aurora.earthmc.net:25577, aurora.earthmc.net",
			"EARTHMC.NET., earthmc.net",
			"play.earthmc.net., play.earthmc.net",
	})
	void normalizesConnectAddresses(String address, String expected) {
		assertEquals(expected, ServerIdentity.normalizeHost(address));
	}

	@Test
	void blankAddressesHaveNoIdentity() {
		assertEquals("", ServerIdentity.normalizeHost(null));
		assertEquals("", ServerIdentity.normalizeHost("   "));
		assertEquals(Optional.empty(), ServerIdentity.of(null));
		assertEquals(Optional.empty(), ServerIdentity.of("  "));
	}

	@Test
	void derivesIdentityFromAddress() {
		Optional<ServerIdentity> identity = ServerIdentity.of("Aurora.EarthMC.net:25565");
		assertEquals(Optional.of(new ServerIdentity("aurora.earthmc.net")), identity);
	}

	@Test
	void blankHostIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new ServerIdentity(" "));
		assertThrows(IllegalArgumentException.class, () -> new ServerIdentity(null));
	}

	@Test
	void fileSafeNameKeepsNormalHostsIntact() {
		assertEquals("aurora.earthmc.net", new ServerIdentity("aurora.earthmc.net").fileSafeName());
	}

	@Test
	void fileSafeNameReplacesExoticCharacters() {
		assertEquals("a_b", new ServerIdentity("a/b").fileSafeName());
		assertEquals("a..b", new ServerIdentity("a..b").fileSafeName());
	}
}
