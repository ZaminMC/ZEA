package net.zaminmc.zea.chat.history;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistoryEntryTest {
	@Test
	void projectsClassifiedMessagesWithFormatting() {
		Component original = Component.literal("")
				.append(Component.literal("Zamin").withStyle(s -> s.withColor(0xFFAA00)))
				.append(Component.literal(" hi"));
		ClassifiedMessage message = new ClassifiedMessage(
				CapturedMessage.of(original, Instant.ofEpochMilli(1_000)),
				MessageCategory.GLOBAL, MessagePayload.None.INSTANCE);

		HistoryEntry entry = HistoryEntry.of(message);

		assertEquals(Instant.ofEpochMilli(1_000), entry.receivedAt());
		assertEquals(MessageCategory.GLOBAL, entry.category());
		assertEquals("Zamin hi", entry.plainText());
		assertEquals("§6Zamin§r hi", entry.formattedText());
	}

	@Test
	void unclassifiedMessagesKeepTheirCategory() {
		ClassifiedMessage message = ClassifiedMessage.unclassified(
				CapturedMessage.of(Component.literal("mystery"), Instant.EPOCH));

		HistoryEntry entry = HistoryEntry.of(message);

		assertEquals(MessageCategory.UNCLASSIFIED, entry.category());
		assertEquals("mystery", entry.plainText());
		assertEquals("mystery", entry.formattedText());
	}

	@Test
	void rejectsNullFields() {
		Instant now = Instant.EPOCH;
		assertThrows(NullPointerException.class,
				() -> new HistoryEntry(null, MessageCategory.GLOBAL, "a", "a"));
		assertThrows(NullPointerException.class,
				() -> new HistoryEntry(now, null, "a", "a"));
		assertThrows(NullPointerException.class,
				() -> new HistoryEntry(now, MessageCategory.GLOBAL, null, "a"));
		assertThrows(NullPointerException.class,
				() -> new HistoryEntry(now, MessageCategory.GLOBAL, "a", null));
	}
}
