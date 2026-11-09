package net.zaminmc.zea.chat.capture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.zaminmc.zea.VanillaBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips through the same codec pair capture uses: encode with
 * plain JSON ops, decode back, compare what EarthMC chat is built from —
 * text, colors, flags, hover text and click commands.
 */
class ComponentJsonTest {
	@BeforeAll
	static void bootstrapVanilla() {
		VanillaBootstrap.ensure();
	}

	@Test
	void plainLiteralRoundTrips() {
		ComponentJson.Result result = ComponentJson.serialize(Component.literal("hi"));

		assertTrue(result.ok());
		assertEquals("hi", result.json().getAsString());
		assertEquals("hi", decode(result.json()).getString());
	}

	@Test
	void styledComponentWithEventsRoundTrips() {
		MutableComponent badge = Component.literal("\uD83C\uDF0E ").withStyle(style -> style
				.withBold(true)
				.withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA))
				.withClickEvent(new ClickEvent.RunCommand("/namemc"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("The Globe"))));
		Component message = badge.append(Component.literal("rest").withStyle(ChatFormatting.GOLD));

		ComponentJson.Result result = ComponentJson.serialize(message);

		assertTrue(result.ok());
		Component decoded = decode(result.json());
		assertEquals("\uD83C\uDF0E rest", decoded.getString());
		assertTrue(decoded.getStyle().isBold());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), decoded.getStyle().getColor());
		ClickEvent.RunCommand click = assertInstanceOf(ClickEvent.RunCommand.class,
				decoded.getStyle().getClickEvent());
		assertEquals("/namemc", click.command());
		HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class,
				decoded.getStyle().getHoverEvent());
		assertEquals("The Globe", hover.value().getString());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD),
				decoded.getSiblings().get(0).getStyle().getColor());
	}

	private static Component decode(JsonElement json) {
		return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
				.result()
				.orElseThrow(() -> new AssertionError("captured JSON did not decode"));
	}
}
