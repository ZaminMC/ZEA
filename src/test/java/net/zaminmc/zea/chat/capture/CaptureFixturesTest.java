package net.zaminmc.zea.chat.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.VanillaBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loop a capture exists for: record a component in memory of the
 * recorder, stop, then load the file back as fixtures — the same path a
 * file captured in-game takes into a regression test.
 */
class CaptureFixturesTest {
	@BeforeAll
	static void bootstrapVanilla() {
		VanillaBootstrap.ensure();
	}

	@TempDir
	Path dir;

	@Test
	void recorderOutputLoadsBackAsComponentFixtures() throws IOException {
		CaptureRecorder recorder = new CaptureRecorder(dir.resolve("capture"), ComponentJson::serialize,
				Runnable::run, Runnable::run, stopped -> { });
		MutableComponent badge = Component.literal("\uD83C\uDF0E ").withStyle(style -> style
				.withBold(true)
				.withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA))
				.withClickEvent(new ClickEvent.RunCommand("/namemc"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("The Globe"))));
		Component message = badge.append(Component.literal("rest").withStyle(ChatFormatting.GOLD));

		Path file = recorder.start();
		recorder.onClassified(ClassifiedMessage.unclassified(CapturedMessage.of(message)));
		recorder.stop();

		Path written;
		try (Stream<Path> files = Files.list(dir.resolve("capture"))) {
			written = files.findFirst().orElseThrow(() -> new AssertionError("no capture file written"));
		}
		assertEquals(file, written);

		List<CaptureFixtures.Fixture> fixtures = CaptureFixtures.load(written);

		assertEquals(1, fixtures.size());
		CaptureFixtures.Fixture fixture = fixtures.get(0);
		assertEquals("UNCLASSIFIED", fixture.category());
		assertEquals("\uD83C\uDF0E rest", fixture.text());
		assertEquals("\uD83C\uDF0E rest", fixture.component().getString());
		assertTrue(fixture.component().getStyle().isBold());
		ClickEvent.RunCommand click = assertInstanceOf(ClickEvent.RunCommand.class,
				fixture.component().getStyle().getClickEvent());
		assertEquals("/namemc", click.command());
		HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class,
				fixture.component().getStyle().getHoverEvent());
		assertEquals("The Globe", hover.value().getString());
	}
}
