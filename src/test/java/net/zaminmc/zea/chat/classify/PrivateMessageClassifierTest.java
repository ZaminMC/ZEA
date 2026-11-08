package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.PrivateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateMessageClassifierTest {
	private final PrivateMessageClassifier classifier = new PrivateMessageClassifier();

	private CapturedMessage capture(String text) {
		return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
	}

	@Test
	void parsesIncoming() {
		var result = classifier.tryClassify(capture("[FORDaLoZ → me] hi"));

		PrivateMessage pm = assertInstanceOf(PrivateMessage.class, result.orElseThrow().payload());
		assertEquals("FORDaLoZ", pm.sender());
		assertEquals("me", pm.recipient());
		assertEquals(PrivateMessage.Direction.INCOMING, pm.direction());
		assertEquals("hi", pm.content());
	}

	@Test
	void parsesOutgoing() {
		var result = classifier.tryClassify(capture("[me → FORDaLoZ] please reply with hi"));

		PrivateMessage pm = assertInstanceOf(PrivateMessage.class, result.orElseThrow().payload());
		assertEquals("me", pm.sender());
		assertEquals("FORDaLoZ", pm.recipient());
		assertEquals(PrivateMessage.Direction.OUTGOING, pm.direction());
		assertEquals("please reply with hi", pm.content());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"[Discord] GavriloPrincip → everyone: hi",
			"trade offers → open now",
			"[not a → real pm] whatever",
			"[way_too_long_username_here → me] hi",
	})
	void declinesNonPrivateMessages(String text) {
		assertTrue(classifier.tryClassify(capture(text)).isEmpty(), text);
	}
}
