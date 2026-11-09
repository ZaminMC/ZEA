package net.zaminmc.zea.chat.capture;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * The capture commands:
 *
 * <pre>
 *   /zea capture on
 *   /zea capture off
 *   /zea capture status
 * </pre>
 *
 * <p>Capture exists to produce component-level ground truth on EarthMC,
 * so {@code on} refuses elsewhere; {@code off} and {@code status} work
 * anywhere so a session is never stuck unreadable.
 */
public final class CaptureCommands {
	private CaptureCommands() {
	}

	/**
	 * Adds the capture branch to the {@code /zea} root.
	 *
	 * @param root      the {@code /zea} root builder, assembled by the entrypoint
	 * @param recorder  the capture session recorder
	 * @param onEarthMc whether the client is currently on EarthMC
	 */
	public static void register(LiteralArgumentBuilder<FabricClientCommandSource> root,
			CaptureRecorder recorder, BooleanSupplier onEarthMc) {
		root.then(ClientCommands.literal("capture")
				.executes(context -> status(context.getSource(), recorder))
				.then(ClientCommands.literal("on")
						.executes(context -> start(context.getSource(), recorder, onEarthMc)))
				.then(ClientCommands.literal("off")
						.executes(context -> stop(context.getSource(), recorder)))
				.then(ClientCommands.literal("status")
						.executes(context -> status(context.getSource(), recorder))));
	}

	private static int start(FabricClientCommandSource source, CaptureRecorder recorder,
			BooleanSupplier onEarthMc) {
		if (!onEarthMc.getAsBoolean()) {
			source.sendError(Component.literal("Capturing only works on EarthMC."));
			return 0;
		}
		Path file = recorder.start();
		if (file == null) {
			source.sendError(Component.literal("Already capturing — run /zea capture off first."));
			return 0;
		}
		source.sendFeedback(Component.literal("Capturing chat to " + file + ". Run /zea capture off to finish,"
				+ " or it stops on its own after " + CaptureRecorder.MAX_ENTRIES + " messages."));
		return 1;
	}

	private static int stop(FabricClientCommandSource source, CaptureRecorder recorder) {
		CaptureRecorder.Stopped stopped = recorder.stop();
		if (stopped == null) {
			source.sendError(Component.literal("No capture is running."));
			return 0;
		}
		source.sendFeedback(Component.literal("Captured " + stopped.recorded() + " message"
				+ (stopped.recorded() == 1 ? "" : "s") + " to " + stopped.file() + "."));
		return 1;
	}

	private static int status(FabricClientCommandSource source, CaptureRecorder recorder) {
		CaptureRecorder.Status status = recorder.status();
		if (!status.active()) {
			source.sendFeedback(Component.literal("No capture running. Start one with /zea capture on."));
		} else {
			source.sendFeedback(Component.literal("Capturing — " + status.recorded() + " of "
					+ status.capacity() + " messages so far, file: " + status.file() + "."));
		}
		return 1;
	}
}
