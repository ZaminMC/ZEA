package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.zaminmc.zea.chat.model.Affiliation;
import net.zaminmc.zea.chat.model.Badge;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ChannelChat;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessagePayload;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalChatClassifierTest {
	private final GlobalChatClassifier classifier = new GlobalChatClassifier();

	private CapturedMessage capture(String text) {
		return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
	}

	@Test
	void parsesObservedGlobalMessage() {
		var result = classifier.tryClassify(capture(
				"🌎❣ [Yugoslavia|Pest] GavriloPrincip: lemme speak with his driver rq"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertEquals(Optional.of("Yugoslavia"), chat.affiliation().nation());
		assertEquals(Optional.of("Pest"), chat.affiliation().town());
		assertEquals("GavriloPrincip", chat.displayName());
		assertEquals("lemme speak with his driver rq", chat.content());
	}

	@Test
	void parsesMessageWithoutBadges() {
		var result = classifier.tryClassify(capture("[Yugoslavia|Pest] GavriloPrincip: hi"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertTrue(chat.badges().isEmpty());
	}

	@Test
	void keepsSingleSegmentAffiliationRaw() {
		var result = classifier.tryClassify(capture("[Pest] GavriloPrincip: hi"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		Affiliation affiliation = chat.affiliation();
		assertEquals("Pest", affiliation.raw());
		assertTrue(affiliation.nation().isEmpty());
		assertTrue(affiliation.town().isEmpty());
	}

	@Test
	void contentMayContainColons() {
		var result = classifier.tryClassify(capture("[Yugoslavia|Pest] Name: time is 12:30 ok"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertEquals("time is 12:30 ok", chat.content());
	}

	@Test
	void displayNameMayBeANickname() {
		var result = classifier.tryClassify(capture("[Yugoslavia|Pest] Coriolanus: hello"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertEquals("Coriolanus", chat.displayName());
	}

	@Test
	void declinesMessagesWithoutBracket() {
		assertTrue(classifier.tryClassify(capture("GavriloPrincip: lemme speak")).isEmpty());
	}

	@Test
	void declinesMessagesWithoutColon() {
		assertTrue(classifier.tryClassify(capture("[Yugoslavia|Pest] GavriloPrincip")) .isEmpty());
	}

	@Test
	void declinesBlankAffiliation() {
		assertTrue(classifier.tryClassify(capture("[] Name: hi")).isEmpty());
	}

	@Test
	void extractsBadgesAsComponentsWithEventsPreserved() {
		Component globe = Component.literal("🌎")
				.setStyle(Style.EMPTY.withHoverEvent(
						new HoverEvent.ShowText(Component.literal("EarthMC 10th anniversary Globe"))));
		Component snowstorm = Component.literal("🔥").setStyle(Style.EMPTY
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Snowstorm badge")))
				.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://store.earthmc.net/"))));
		Component message = Component.empty()
				.append(globe)
				.append(snowstorm)
				.append("[Yugoslavia|Pest] GavriloPrincip: hi");

		var result = classifier.tryClassify(CapturedMessage.of(message, Instant.EPOCH));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertEquals(2, chat.badges().size());
		Badge first = chat.badges().get(0);
		assertEquals("🌎", first.symbol());
		assertEquals(Optional.of("anniversary-globe"), first.knownId());
		assertSame(globe, first.original().orElseThrow());
		assertInstanceOf(HoverEvent.ShowText.class, first.original().orElseThrow().getStyle().getHoverEvent());
		Badge second = chat.badges().get(1);
		assertEquals("🔥", second.symbol());
		assertEquals(Optional.of("snowstorm"), second.knownId());
		ClickEvent click = second.original().orElseThrow().getStyle().getClickEvent();
		ClickEvent.OpenUrl openUrl = assertInstanceOf(ClickEvent.OpenUrl.class, click);
		assertEquals(URI.create("https://store.earthmc.net/"), openUrl.uri());
	}

	@Test
	void flatMessageFallsBackToKnownSymbolMatching() {
		var result = classifier.tryClassify(capture("🌎❣ [Yugoslavia|Pest] GavriloPrincip: hi"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertEquals(2, chat.badges().size());
		assertEquals("🌎", chat.badges().get(0).symbol());
		assertEquals(Optional.of("anniversary-globe"), chat.badges().get(0).knownId());
		assertEquals("❣", chat.badges().get(1).symbol());
		assertEquals(Optional.of("server-heart"), chat.badges().get(1).knownId());
		assertTrue(chat.badges().get(0).original().isEmpty());
	}

	@Test
	void unknownLeadingSymbolsAreNotBadges() {
		var result = classifier.tryClassify(capture("⍰⍰ [Yugoslavia|Pest] GavriloPrincip: hi"));

		ChannelChat chat = assertInstanceOf(ChannelChat.class, result.orElseThrow().payload());
		assertTrue(chat.badges().isEmpty());
	}

	@Test
	void classifiedMessageRetainsOriginalComponent() {
		Component original = Component.literal("🌎 [Yugoslavia|Pest] GavriloPrincip: hi");
		CapturedMessage captured = CapturedMessage.of(original, Instant.EPOCH);

		ClassifiedMessage classified = new ClassificationPipeline().classify(captured);

		assertSame(original, classified.captured().original());
		assertInstanceOf(ChannelChat.class, classified.payload());
	}
}
