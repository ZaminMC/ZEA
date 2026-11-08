package net.zaminmc.zea.chat.feeds;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
