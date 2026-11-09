package net.zaminmc.zea.chat.feeds;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.zaminmc.zea.chat.classify.ClassificationCache;
import net.zaminmc.zea.chat.classify.ClassificationPipeline;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedFilterTest {
	private ClassifiedMessage classified(MessageCategory category) {
		CapturedMessage captured = CapturedMessage.of(Component.literal("sample"), Instant.EPOCH);
		return new ClassifiedMessage(captured, category, MessagePayload.None.INSTANCE);
	}

	@Test
	void allAggregatesEveryCategory() {
		for (MessageCategory category : MessageCategory.values()) {
			assertTrue(Feeds.ALL.shows(classified(category)), category.name());
		}
	}

	@ParameterizedTest
	@EnumSource(value = MessageCategory.class, names = {"GLOBAL", "LOCAL", "TRADE", "NATION", "TOWN"})
	void channelFeedsShowOnlyTheirCategory(MessageCategory category) {
		Feed feed = Feeds.allFeeds().stream()
				.filter(f -> f.id().equals(category.name().toLowerCase()))
				.findFirst().orElseThrow();

		assertTrue(feed.shows(classified(category)));
		for (MessageCategory other : MessageCategory.values()) {
			if (other != category) {
				assertFalse(feed.shows(classified(other)), category + " feed shows " + other);
			}
		}
	}

	@Test
	void unclassifiedFeedKeepsUnknownMessages() {
		assertTrue(Feeds.UNCLASSIFIED.shows(classified(MessageCategory.UNCLASSIFIED)));
		assertFalse(Feeds.GLOBAL.shows(classified(MessageCategory.UNCLASSIFIED)));
	}

	@Test
	void defaultTabsAreTheImportantOnes() {
		List<String> ids = Feeds.defaultVisibleTabs().stream().map(Feed::id).toList();

		assertEquals(List.of("all", "global", "local", "trade", "nation", "town", "pms"), ids);
	}

	@Test
	void extraFeedsRemainAccessible() {
		List<String> ids = Feeds.extraFeeds().stream().map(Feed::id).toList();

		assertEquals(List.of("discord", "votes", "crates", "deaths", "system", "unclassified"), ids);
	}

	@Test
	void everyCategoryHasAHome() {
		for (MessageCategory category : MessageCategory.values()) {
			boolean visibleSomewhere = Feeds.allFeeds().stream()
					.anyMatch(feed -> feed.shows(classified(category)));
			assertTrue(visibleSomewhere, category + " has no feed");
		}
	}

	@Test
	void visibleTabsDefaultToTheImportantOnes() {
		List<String> ids = Feeds.visibleTabs(Set.of()).stream().map(Feed::id).toList();

		assertEquals(List.of("all", "global", "local", "trade", "nation", "town", "pms"), ids);
	}

	@Test
	void enabledExtrasAppendAfterTheDefaultTabs() {
		List<String> ids = Feeds.visibleTabs(Set.of("deaths", "discord")).stream()
				.map(Feed::id).toList();

		assertEquals(List.of("all", "global", "local", "trade", "nation", "town", "pms",
				"discord", "deaths"), ids);
	}

	@Test
	void unknownEnabledIdsAreIgnored() {
		List<String> ids = Feeds.visibleTabs(Set.of("nonsense", "votes")).stream()
				.map(Feed::id).toList();

		assertEquals(List.of("all", "global", "local", "trade", "nation", "town", "pms", "votes"), ids);
	}

	private final ClassificationCache cache = new ClassificationCache(new ClassificationPipeline(), 64);
	private final FeedSelection selection = new FeedSelection(Feeds.ALL);
	private final FeedFilter filter = new FeedFilter(cache, selection);

	private GuiMessage message(String text) {
		return new GuiMessage(0, Component.literal(text), new MessageSignature(new byte[256]),
				GuiMessageSource.SYSTEM_SERVER, GuiMessageTag.system());
	}

	@Test
	void allTabUsesNoFilterSoVanillaShowsEverything() {
		assertNull(filter.currentFilter());
	}

	@Test
	void selectedFeedShowsOnlyItsMessages() {
		selection.select(Feeds.VOTES);
		Predicate<GuiMessage> predicate = filter.currentFilter();
		assertNotNull(predicate);

		assertTrue(predicate.test(message("AdamGladio voted and received a gold crate /vote")));
		assertFalse(predicate.test(message("[Yugoslavia|Pest] GavriloPrincip: hello")));
		assertFalse(predicate.test(message("completely unknown structure")));
	}

	@Test
	void unclassifiedMessagesStayVisibleOnlyInAggregateAndGeneralFeeds() {
		selection.select(Feeds.UNCLASSIFIED);
		Predicate<GuiMessage> general = filter.currentFilter();

		assertTrue(general.test(message("completely unknown structure")));
		assertFalse(general.test(message("AdamGladio voted and received a gold crate /vote")));

		selection.select(Feeds.VOTES);
		assertFalse(filter.currentFilter().test(message("completely unknown structure")));
	}

	@Test
	void filterRecognizesMessagesSeenByTheCacheFirst() {
		Component death = Component.literal("☠ xfw_x was killed by dex247");
		cache.classificationOf(death);

		selection.select(Feeds.DEATHS);
		assertTrue(filter.currentFilter().test(new GuiMessage(0, death, null,
				GuiMessageSource.SYSTEM_SERVER, null)));
	}

	@Test
	void openSearchWithBlankQueryFiltersNothing() {
		filter.setSearchQuery("");

		assertNull(filter.currentFilter());
	}

	@Test
	void searchOnAllNarrowsByText() {
		filter.setSearchQuery("gravel");

		Predicate<GuiMessage> predicate = filter.currentFilter();
		assertNotNull(predicate);
		assertTrue(predicate.test(message("[Germany|Berlin] KoenigLuis: selling gravel cheap")));
		assertFalse(predicate.test(message("[Germany|Berlin] KoenigLuis: selling dirt cheap")));
	}

	@Test
	void searchMatchesCaseInsensitively() {
		filter.setSearchQuery("koENig");

		assertTrue(filter.currentFilter().test(message("[Germany|Berlin] KoenigLuis: hello")));
	}

	@Test
	void searchComposesWithTheSelectedFeed() {
		filter.setSearchQuery("gravel");
		selection.select(Feeds.GLOBAL);

		Predicate<GuiMessage> predicate = filter.currentFilter();
		// Matches the text but not the feed: stays hidden.
		assertFalse(predicate.test(message("AdamGladio voted and received gravel")));
		// Matches the feed but not the text: stays hidden.
		assertFalse(predicate.test(message("[Germany|Berlin] KoenigLuis: selling dirt cheap")));
		// Matches both: shown.
		assertTrue(predicate.test(message("[Germany|Berlin] KoenigLuis: selling gravel cheap")));
	}

	@Test
	void searchMatchesFlattenedStyledText() {
		net.minecraft.network.chat.MutableComponent name = Component.literal("Aurora")
				.withStyle(net.minecraft.ChatFormatting.AQUA);
		net.minecraft.network.chat.MutableComponent message = Component.literal(": anyone selling gravel?")
				.withStyle(net.minecraft.ChatFormatting.WHITE);
		filter.setSearchQuery("selling gravel");

		assertTrue(filter.currentFilter().test(new GuiMessage(0, name.append(message), null,
				GuiMessageSource.SYSTEM_SERVER, null)));
	}

	@Test
	void closingTheSearchRestoresTheFeedFilterOnly() {
		filter.setSearchQuery("gravel");
		selection.select(Feeds.VOTES);
		filter.setSearchQuery(null);

		Predicate<GuiMessage> predicate = filter.currentFilter();
		assertNotNull(predicate);
		assertTrue(predicate.test(message("AdamGladio voted and received a gold crate /vote")));
		assertFalse(predicate.test(message("[Germany|Berlin] KoenigLuis: selling gravel cheap")));
	}
}
