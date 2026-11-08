package net.zaminmc.zea.chat.classify;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression fixtures for every known EarthMC message family, run through
 * the full pipeline. Recorded observations live in
 * {@code fixtures/messages.json}; component-structure behavior (badges,
 * hover/click preservation) is covered by the per-classifier tests.
 */
class ClassificationFixturesTest {
	private static final Gson GSON = new Gson();
	private static final Type FIXTURE_LIST = new TypeToken<List<Fixture>>() {
	}.getType();

	private record Fixture(String family, String text, String category) {
	}

	private static List<Fixture> fixtures;
	private static ClassificationPipeline pipeline;

	@BeforeAll
	static void loadFixtures() {
		try (InputStream in = ClassificationFixturesTest.class.getResourceAsStream("/fixtures/messages.json")) {
			requireNonNull(in, "fixtures/messages.json not found");
			fixtures = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), FIXTURE_LIST);
		} catch (Exception e) {
			throw new IllegalStateException("could not load fixtures", e);
		}
		pipeline = new ClassificationPipeline();
	}

	static Stream<Fixture> fixtureSource() {
		return fixtures.stream();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("fixtureSource")
	void classifiesFamily(Fixture fixture) {
		CapturedMessage captured = CapturedMessage.of(Component.literal(fixture.text()), Instant.EPOCH);

		ClassifiedMessage classified = pipeline.classify(captured);

		assertEquals(MessageCategory.valueOf(fixture.category()), classified.category(), fixture.text());
	}

	@ParameterizedTest(name = "{0} keeps original")
	@MethodSource("fixtureSource")
	void originalComponentAlwaysRetained(Fixture fixture) {
		CapturedMessage captured = CapturedMessage.of(Component.literal(fixture.text()), Instant.EPOCH);

		ClassifiedMessage classified = pipeline.classify(captured);

		assertSame(captured.original(), classified.captured().original());
		assertEquals(fixture.text(), classified.captured().text());
	}
}
