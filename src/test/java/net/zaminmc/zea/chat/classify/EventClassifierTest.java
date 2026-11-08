package net.zaminmc.zea.chat.classify;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.CrateWinMessage;
import net.zaminmc.zea.chat.model.DeathMessage;
import net.zaminmc.zea.chat.model.VoteMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventClassifierTest {
	private final DeathClassifier death = new DeathClassifier();
	private final VoteClassifier vote = new VoteClassifier();
	private final CrateWinClassifier crateWin = new CrateWinClassifier();

	private CapturedMessage capture(String text) {
		return CapturedMessage.of(Component.literal(text), Instant.EPOCH);
	}

	@Test
	void parsesDeath() {
		var result = death.tryClassify(capture("☠ xfw_x was killed by dex247"));

		DeathMessage payload = assertInstanceOf(DeathMessage.class, result.orElseThrow().payload());
		assertEquals("xfw_x", payload.victim());
		assertEquals("dex247", payload.killer());
	}

	@Test
	void deathHoverInformationIsPreservedOnTheOriginal() {
		Component original = Component.literal("☠ CherryPinkVelvet was killed by FORDaLoZ")
				.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(
						Component.literal("Damage dealt: FORDaLoZ: 8.5 hearts (100%)"))));
		CapturedMessage captured = CapturedMessage.of(original, Instant.EPOCH);

		var result = death.tryClassify(captured);

		assertInstanceOf(DeathMessage.class, result.orElseThrow().payload());
		assertSame(original, captured.original());
		HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class,
				captured.original().getStyle().getHoverEvent());
		assertEquals("Damage dealt: FORDaLoZ: 8.5 hearts (100%)", hover.value().getString());
	}

	@Test
	void declinesOtherDeathPhrasings() {
		assertTrue(death.tryClassify(capture("☠ player starved to death")).isEmpty());
		assertTrue(death.tryClassify(capture("player was killed by player")).isEmpty());
	}

	@Test
	void parsesVote() {
		var result = vote.tryClassify(capture("AdamGladio voted and received a gold crate /vote"));

		VoteMessage payload = assertInstanceOf(VoteMessage.class, result.orElseThrow().payload());
		assertEquals("AdamGladio", payload.player());
		assertEquals("gold crate", payload.reward());
	}

	@Test
	void voteSuffixIsOptional() {
		var result = vote.tryClassify(capture("AdamGladio voted and received a vote crate"));

		VoteMessage payload = assertInstanceOf(VoteMessage.class, result.orElseThrow().payload());
		assertEquals("vote crate", payload.reward());
	}

	@Test
	void parsesCrateWinsWithVariableAmountsAndNames() {
		var bruhBox = crateWin.tryClassify(capture("akirarocks won 16 gold from a bruh box"));
		CrateWinMessage first = assertInstanceOf(CrateWinMessage.class, bruhBox.orElseThrow().payload());
		assertEquals("akirarocks", first.player());
		assertEquals(16, first.amount());
		assertEquals("gold", first.currency());
		assertEquals("bruh box", first.crateName());

		var goldCrate = crateWin.tryClassify(capture("jediknight7491 won 32 gold from a gold crate /vote"));
		CrateWinMessage second = assertInstanceOf(CrateWinMessage.class, goldCrate.orElseThrow().payload());
		assertEquals(32, second.amount());
		assertEquals("gold crate", second.crateName());
	}

	@Test
	void declinesLookalikes() {
		assertTrue(vote.tryClassify(capture("AdamGladio voted and that is all")).isEmpty());
		assertTrue(crateWin.tryClassify(capture("akirarocks won the lottery")).isEmpty());
	}
}
