package net.zaminmc.zea.chat.feeds;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the multi-send interaction policy: which feeds can become send
 * targets, and which chat sends become multi-target dispatches. The
 * channel-backed feeds must line up with the {@code Channels} table —
 * feed ids and channel ids drift silently otherwise.
 */
class MultiSendInteractionTest {
	@Test
	void channelFeedsMapToTheirWriteChannel() {
		assertSame(Channels.GLOBAL, MultiSendInteraction.channelOf(Feeds.GLOBAL));
		assertSame(Channels.LOCAL, MultiSendInteraction.channelOf(Feeds.LOCAL));
		assertSame(Channels.TRADE, MultiSendInteraction.channelOf(Feeds.TRADE));
		assertSame(Channels.NATION, MultiSendInteraction.channelOf(Feeds.NATION));
		assertSame(Channels.TOWN, MultiSendInteraction.channelOf(Feeds.TOWN));
	}

	@Test
	void channellessFeedsHaveNoWriteChannel() {
		// All aggregates; the event and system feeds are read-only views.
		assertNull(MultiSendInteraction.channelOf(Feeds.ALL));
		assertNull(MultiSendInteraction.channelOf(Feeds.PRIVATE_MESSAGES));
		assertNull(MultiSendInteraction.channelOf(Feeds.DEATHS));
		assertNull(MultiSendInteraction.channelOf(Feeds.SYSTEM));
		assertNull(MultiSendInteraction.channelOf(Feeds.UNCLASSIFIED));
	}

	@Test
	void togglingAChannelFeedAddsThenRemovesIt() {
		SendTargets targets = new SendTargets();
		assertEquals(MultiSendInteraction.ToggleOutcome.ADDED,
				MultiSendInteraction.toggleTarget(targets, Feeds.TRADE, true));
		assertTrue(targets.isSelected(Channels.TRADE));
		assertEquals(MultiSendInteraction.ToggleOutcome.REMOVED,
				MultiSendInteraction.toggleTarget(targets, Feeds.TRADE, true));
		assertTrue(targets.isEmpty());
	}

	@Test
	void togglingAChannellessFeedNeverDisturbsTheSelection() {
		SendTargets targets = new SendTargets();
		MultiSendInteraction.toggleTarget(targets, Feeds.GLOBAL, true);
		assertEquals(MultiSendInteraction.ToggleOutcome.NO_CHANNEL,
				MultiSendInteraction.toggleTarget(targets, Feeds.ALL, true));
		assertEquals(MultiSendInteraction.ToggleOutcome.NO_CHANNEL,
				MultiSendInteraction.toggleTarget(targets, Feeds.VOTES, true));
		// The earlier selection survives both refusals.
		assertTrue(targets.isSelected(Channels.GLOBAL));
	}

	@Test
	void togglingWhileDisabledRefusesWithoutDisturbingTheSelection() {
		SendTargets targets = new SendTargets();
		MultiSendInteraction.toggleTarget(targets, Feeds.GLOBAL, true);
		assertEquals(MultiSendInteraction.ToggleOutcome.DISABLED,
				MultiSendInteraction.toggleTarget(targets, Feeds.TOWN, false));
		assertFalse(targets.isSelected(Channels.TOWN));
		assertTrue(targets.isSelected(Channels.GLOBAL));
	}

	@Test
	void shouldInterceptRequiresEnabledTargetsAndPlainText() {
		SendTargets targets = new SendTargets();
		MultiSendInteraction.toggleTarget(targets, Feeds.GLOBAL, true);

		assertTrue(MultiSendInteraction.shouldIntercept(targets, true, "hello town"));
		assertFalse(MultiSendInteraction.shouldIntercept(targets, false, "hello town"));
		assertFalse(MultiSendInteraction.shouldIntercept(targets, true, "/g hello"));
		assertFalse(MultiSendInteraction.shouldIntercept(targets, true, ""));
		assertFalse(MultiSendInteraction.shouldIntercept(targets, true, "   "));
		assertFalse(MultiSendInteraction.shouldIntercept(targets, true, null));
	}

	@Test
	void emptySelectionNeverIntercepts() {
		assertFalse(MultiSendInteraction.shouldIntercept(new SendTargets(), true, "hello"));
	}

	@Test
	void targetedRenderingFollowsSelectionAndTheConfigFlag() {
		SendTargets targets = new SendTargets();
		MultiSendInteraction.toggleTarget(targets, Feeds.NATION, true);

		assertTrue(MultiSendInteraction.isTargeted(targets, true, Feeds.NATION));
		assertFalse(MultiSendInteraction.isTargeted(targets, true, Feeds.TOWN));
		assertFalse(MultiSendInteraction.isTargeted(targets, false, Feeds.NATION));
		assertFalse(MultiSendInteraction.isTargeted(targets, true, Feeds.ALL));
	}
}
