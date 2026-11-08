package net.zaminmc.zea.chat.model;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapturedMessageTest {
	@Test
	void flattensTextOnce() {
		Component component = Component.empty()
				.append(Component.literal("hello "))
				.append(Component.literal("world"));

		CapturedMessage captured = CapturedMessage.of(component, Instant.EPOCH);

		assertEquals("hello world", captured.text());
		assertSame(component, captured.original());
		assertSame(Instant.EPOCH, captured.receivedAt());
	}

	@Test
	void rejectsNullFields() {
		assertThrows(NullPointerException.class,
				() -> new CapturedMessage(null, "text", Instant.EPOCH));
		assertThrows(NullPointerException.class,
				() -> new CapturedMessage(Component.literal("x"), null, Instant.EPOCH));
		assertThrows(NullPointerException.class,
				() -> new CapturedMessage(Component.literal("x"), "text", null));
	}
}
