package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationCacheTest {
	@Test
	void remembersClassificationByComponentIdentity() {
		ClassificationCache cache = new ClassificationCache(new ClassificationPipeline(), 16);
		Component content = Component.literal("[FORDaLoZ → me] hi");
		ClassifiedMessage classified = cache.classify(CapturedMessage.of(content, Instant.EPOCH));

		Optional<ClassifiedMessage> lookedUp = cache.lookup(content);
		assertTrue(lookedUp.isPresent());
		assertEquals(classified, lookedUp.get());
	}

	@Test
	void distinctComponentsWithEqualTextClassifyIndependently() {
		ClassificationCache cache = new ClassificationCache(new ClassificationPipeline(), 16);
		Component first = Component.literal("[FORDaLoZ → me] hi");
		Component second = Component.literal("[FORDaLoZ → me] hi");

		cache.classify(CapturedMessage.of(first, Instant.EPOCH));

		assertTrue(cache.lookup(second).isEmpty(), "identity keying must not merge equal components");
		assertEquals(1, cache.size());
	}

	@Test
	void classificationOfClassifiesAndCachesOnFirstSight() {
		ClassificationCache cache = new ClassificationCache(new ClassificationPipeline(), 16);
		Component vote = Component.literal("AdamGladio voted and received a gold crate /vote");

		ClassifiedMessage first = cache.classificationOf(vote);
		assertEquals(MessageCategory.VOTE, first.category());
		assertTrue(cache.lookup(vote).isPresent(), "on-demand classification is remembered");
		assertEquals(first, cache.classificationOf(vote));
	}

	@Test
	void capacityEvictsOldestEntries() {
		ClassificationCache cache = new ClassificationCache(new ClassificationPipeline(), 2);
		Component one = Component.literal("one");
		Component two = Component.literal("two");
		Component three = Component.literal("three");
		cache.classify(CapturedMessage.of(one, Instant.EPOCH));
		cache.classify(CapturedMessage.of(two, Instant.EPOCH));
		cache.classify(CapturedMessage.of(three, Instant.EPOCH));

		assertEquals(2, cache.size());
		assertTrue(cache.lookup(one).isEmpty());
		assertTrue(cache.lookup(three).isPresent());
	}

	@Test
	void rejectsInvalidConstruction() {
		assertThrows(NullPointerException.class,
				() -> new ClassificationCache(null, 16));
		assertThrows(IllegalArgumentException.class,
				() -> new ClassificationCache(new ClassificationPipeline(), 0));
	}
}
