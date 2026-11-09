package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.TaggedChannelChat;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaggedChannelChatClassifierTest {
	private final TaggedChannelChatClassifier classifier = new TaggedChannelChatClassifier();

	private CapturedMessage capture(String text) {
		return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
	}

	private TaggedChannelChat payload(String text) {
		var result = classifier.tryClassify(capture(text));
		return assertInstanceOf(TaggedChannelChat.class, result.orElseThrow().payload());
	}

	@Test
	void parsesObservedLocalMessage() {
		var result = classifier.tryClassify(capture("[Local] Abasing: yo"));

		assertEquals(MessageCategory.LOCAL, result.orElseThrow().category());
		TaggedChannelChat chat = payload("[Local] Abasing: yo");
		assertEquals("Local", chat.tag());
		assertTrue(chat.town().isEmpty());
		assertEquals("Abasing", chat.displayName());
		assertEquals("yo", chat.content());
	}

	@Test
	void parsesObservedTradeMessage() {
		var result = classifier.tryClassify(capture(
				"[Trade] Abasing: Everyone Say Cookie, and see what happens"));

		assertEquals(MessageCategory.TRADE, result.orElseThrow().category());
		TaggedChannelChat chat = payload("[Trade] Abasing: Everyone Say Cookie, and see what happens");
		assertEquals("Trade", chat.tag());
		assertEquals("Everyone Say Cookie, and see what happens", chat.content());
	}

	@Test
	void parsesObservedNationMessageWithTown() {
		var result = classifier.tryClassify(capture("[Nation Chat|Sosnovka] Abasing: yo"));

		assertEquals(MessageCategory.NATION, result.orElseThrow().category());
		TaggedChannelChat chat = payload("[Nation Chat|Sosnovka] Abasing: yo");
		assertEquals("Nation Chat", chat.tag());
		assertEquals(Optional.of("Sosnovka"), chat.town());
		assertEquals("Abasing", chat.displayName());
		assertEquals("yo", chat.content());
	}

	@Test
	void parsesObservedTownMessage() {
		var result = classifier.tryClassify(capture("[Town Chat] Abasing: YOOO!"));

		assertEquals(MessageCategory.TOWN, result.orElseThrow().category());
		TaggedChannelChat chat = payload("[Town Chat] Abasing: YOOO!");
		assertEquals("Town Chat", chat.tag());
		assertTrue(chat.town().isEmpty());
		assertEquals("YOOO!", chat.content());
	}

	@Test
	void nationTagWithoutTownIsDeclined() {
		// Unobserved shape: never guessed.
		assertTrue(classifier.tryClassify(capture("[Nation Chat] Abasing: yo")).isEmpty());
	}

	@Test
	void globalAffiliationsAreLeftToTheGlobalClassifier() {
		assertTrue(classifier.tryClassify(capture(
				"[Yugoslavia|Pest] GavriloPrincip: goring*")).isEmpty());
		assertTrue(classifier.tryClassify(capture("[Pest] GavriloPrincip: hi")).isEmpty());
	}

	@Test
	void declinesUnknownTags() {
		assertTrue(classifier.tryClassify(capture("[Party] Name: hi")).isEmpty());
		assertTrue(classifier.tryClassify(capture("[Discord] Name: hi")).isEmpty());
	}

	@Test
	void tagsAreCaseSensitive() {
		assertTrue(classifier.tryClassify(capture("[local] Abasing: yo")).isEmpty());
		assertTrue(classifier.tryClassify(capture("[TOWN CHAT] Abasing: yo")).isEmpty());
	}

	@Test
	void declinesOverlongTowns() {
		String town = "a".repeat(65);
		assertTrue(classifier.tryClassify(capture("[Nation Chat|" + town + "] Name: hi")).isEmpty());
	}

	@Test
	void declinesMissingNameAndColon() {
		assertTrue(classifier.tryClassify(capture("[Local] Abasing yo")).isEmpty());
		assertTrue(classifier.tryClassify(capture("[Local] Abasing:")).isEmpty());
	}

	@Test
	void declinesNamesContainingBrackets() {
		assertTrue(classifier.tryClassify(capture("[Trade] [VIP] Player: hi")).isEmpty());
	}

	@Test
	void declinesMissingSpaceAfterBracket() {
		assertTrue(classifier.tryClassify(capture("[Local]Abasing: yo")).isEmpty());
	}

	@Test
	void declinesWhenBracketIsNotLeading() {
		assertTrue(classifier.tryClassify(capture("hello [Local] Name: hi")).isEmpty());
	}

	@Test
	void contentMayContainColons() {
		TaggedChannelChat chat = payload("[Local] Name: time is 12:30 ok");

		assertEquals("Name", chat.displayName());
		assertEquals("time is 12:30 ok", chat.content());
	}

	@Test
	void pipelineRoutesTaggedChannelsBeforeGlobal() {
		var pipeline = new ClassificationPipeline();

		var local = pipeline.classify(capture("[Local] Abasing: yo"));
		assertEquals(MessageCategory.LOCAL, local.category());

		var nation = pipeline.classify(capture("[Nation Chat|Sosnovka] Abasing: yo"));
		assertEquals(MessageCategory.NATION, nation.category());
	}
}
