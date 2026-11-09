package net.zaminmc.zea.mention;

import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.feeds.OutgoingMessage;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.config.ZeaConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Auto-response guards: off by default, inert without text,
 * cooldown-limited, session-capped, loop-guarded and channel-safe.
 */
class MentionAutoResponderTest {
	private static final long NOW = 1_000_000;
	private static final String RESPONSE = "busy, will reply soon";

	private final ZeaConfig config = new ZeaConfig();
	private final List<OutgoingMessage> sent = new ArrayList<>();
	private final MentionAutoResponder responder = new MentionAutoResponder(config, sent::add);

	@Test
	void disabledByDefault() {
		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);

		assertEquals(0, sent.size());
		assertEquals(0, responder.responsesThisSession());
	}

	@Test
	void enabledWithTextRespondsOnTheMentionedChannel() {
		enable();

		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);

		assertEquals(1, sent.size());
		assertEquals("g", sent.get(0).command());
		assertEquals(RESPONSE, sent.get(0).arguments());
	}

	@Test
	void blankTextIsInert() {
		config.mentions.autoRespond = true;
		config.mentions.autoResponseText = "   ";

		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);

		assertEquals(0, sent.size());
	}

	@Test
	void cooldownBlocksRapidMentions() {
		enable();

		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);
		responder.onMention(mention("pengpy", "Zamin?", MessageCategory.GLOBAL), NOW + 5_000);

		assertEquals(1, sent.size());
	}

	@Test
	void cooldownExpiryAllowsTheNextResponse() {
		enable();

		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);
		responder.onMention(mention("pengpy", "Zamin?", MessageCategory.GLOBAL), NOW + 30_000);

		assertEquals(2, sent.size());
	}

	@Test
	void sessionCapStopsTheFlood() {
		enable();
		long now = NOW;
		for (int i = 0; i < 25; i++) {
			responder.onMention(mention("Coriolanus", "Zamin #" + i, MessageCategory.GLOBAL), now);
			now += 60_000; // past every cooldown
		}

		assertEquals(20, sent.size(), "the hard session cap wins over any cooldown setting");
	}

	@Test
	void resetRestartsTheSessionCounters() {
		enable();
		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.GLOBAL), NOW);

		responder.reset();

		assertEquals(0, responder.responsesThisSession());
		responder.onMention(mention("pengpy", "Zamin?", MessageCategory.GLOBAL), NOW + 1_000);
		assertEquals(2, sent.size(), "a fresh session also ignores the old cooldown");
	}

	@Test
	void echoOfOurOwnResponseNeverTriggers() {
		enable();

		// Someone's client (or our own echo) shows the response text.
		responder.onMention(mention("Coriolanus", "Zamin " + RESPONSE, MessageCategory.GLOBAL), NOW);

		assertEquals(0, sent.size(), "the loop guard suppresses auto-response ping-pong");
	}

	@Test
	void unattributableChannelsNeverRespond() {
		enable();

		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.UNCLASSIFIED), NOW);
		responder.onMention(mention("Coriolanus", "hey Zamin", MessageCategory.DISCORD), NOW);

		assertEquals(0, sent.size());
	}

	@Test
	void localTradeNationAndTownRouteToTheirChannels() {
		enable();

		responder.onMention(mention("a", "hi Zamin", MessageCategory.LOCAL), NOW);
		responder.onMention(mention("b", "hi Zamin", MessageCategory.TRADE), NOW + 60_000);
		responder.onMention(mention("c", "hi Zamin", MessageCategory.NATION), NOW + 120_000);
		responder.onMention(mention("d", "hi Zamin", MessageCategory.TOWN), NOW + 180_000);

		assertEquals(List.of("local", "tradechat", "nc", "tc"),
				sent.stream().map(OutgoingMessage::command).toList());
	}

	private void enable() {
		config.mentions.autoRespond = true;
		config.mentions.autoResponseText = RESPONSE;
	}

	private Mention mention(String speaker, String content, MessageCategory category) {
		CapturedMessage source = CapturedMessage.of(
				Component.literal(speaker + ": " + content), Instant.EPOCH);
		return new Mention(source, speaker, content, category);
	}
}
