package net.zaminmc.zea.chat.feeds;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutgoingRouterTest {
	private final OutgoingRouter router = new OutgoingRouter();

	@Test
	void routesSingleTargetThroughPerMessageCommand() {
		OutgoingMessage outgoing = router.route(Channels.GLOBAL, "hello world");

		assertEquals("g", outgoing.command());
		assertEquals("hello world", outgoing.arguments());
		assertEquals("g hello world", outgoing.commandLine());
	}

	@Test
	void routesEveryChannel() {
		assertEquals("local", router.route(Channels.LOCAL, "hi").command());
		assertEquals("tradechat", router.route(Channels.TRADE, "hi").command());
		assertEquals("nc", router.route(Channels.NATION, "hi").command());
		assertEquals("tc", router.route(Channels.TOWN, "hi").command());
	}

	@Test
	void multiTargetRoutingPreservesSelectionOrder() {
		List<OutgoingMessage> outgoing = router.route(
				List.of(Channels.TOWN, Channels.GLOBAL, Channels.NATION), "hi");

		assertEquals(List.of("tc", "g", "nc"), outgoing.stream().map(OutgoingMessage::command).toList());
	}

	@Test
	void routingDoesNotTouchServerWritingChannel() {
		// The router only produces commands; ChannelState is a separate
		// concern updated exclusively by server reports (see ChannelStateTest).
		ChannelState state = new ChannelState();

		router.route(Channels.TOWN, "hi");

		assertEquals(java.util.Optional.empty(), state.writeChannel());
	}

	@Test
	void rejectsBlankMessages() {
		assertThrows(IllegalArgumentException.class, () -> router.route(Channels.GLOBAL, "  "));
	}
}
