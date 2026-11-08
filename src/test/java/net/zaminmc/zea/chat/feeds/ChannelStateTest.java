package net.zaminmc.zea.chat.feeds;

import net.zaminmc.zea.chat.model.WritingChannelReport;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelStateTest {
	@Test
	void startsUnknown() {
		ChannelState state = new ChannelState();

		assertEquals(Optional.empty(), state.writeChannel());
	}

	@Test
	void appliesServerReport() {
		ChannelState state = new ChannelState();

		state.applyReport(new WritingChannelReport("global", 5));

		assertEquals(Optional.of("global"), state.writeChannel());
	}

	@Test
	void reportReplacesPreviousChannel() {
		ChannelState state = new ChannelState();
		state.applyReport(new WritingChannelReport("global", 5));

		state.applyReport(new WritingChannelReport("town", 2));

		assertEquals(Optional.of("town"), state.writeChannel());
	}

	@Test
	void disconnectClearsChannel() {
		ChannelState state = new ChannelState();
		state.applyReport(new WritingChannelReport("local", 1));

		state.clear();

		assertEquals(Optional.empty(), state.writeChannel());
	}

	@Test
	void rejectsNullReport() {
		assertThrows(NullPointerException.class, () -> new ChannelState().applyReport(null));
	}
}
