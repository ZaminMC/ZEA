package net.zaminmc.zea.earthmc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthMcDetectorTest {
	@ParameterizedTest
	@CsvSource({
			"earthmc.net",
			"EARTHMC.NET",
			"EarthMC.Net",
			"play.earthmc.net",
			"aurora.earthmc.net",
			"earthmc.net:25565",
			"  earthmc.net  ",
	})
	void acceptsEarthMcAddresses(String address) {
		assertTrue(EarthMcDetector.isEarthMcAddress(address), address);
	}

	@ParameterizedTest
	@CsvSource({
			"hypixel.net",
			"earthmc.network",
			"earthmc.example.com",
			"earthmc.net.evil.com",
			"not-earthmc.net",
	})
	void rejectsOtherAddresses(String address) {
		assertFalse(EarthMcDetector.isEarthMcAddress(address), address);
	}

	@Test
	void rejectsNullAndEmpty() {
		assertFalse(EarthMcDetector.isEarthMcAddress(null));
		assertFalse(EarthMcDetector.isEarthMcAddress(""));
	}
}
