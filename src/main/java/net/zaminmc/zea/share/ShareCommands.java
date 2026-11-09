package net.zaminmc.zea.share;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletionException;

/**
 * The client commands for sharing:
 *
 * <pre>
 *   /zea share &lt;inven|gen&gt;
 *   /zea import &lt;key or paste URL&gt;
 *   /zea import confirm
 *   /zea import cancel
 * </pre>
 *
 * <p>Handlers never block: network work runs on the share executor and
 * feedback arrives as a client message when it completes. Brigadier
 * matches the literal children ({@code confirm}, {@code cancel}) before
 * the greedy paste argument, so a paste key that spells "confirm" is
 * still reachable by pasting the full URL instead.
 */
public final class ShareCommands {
	private ShareCommands() {
	}

	/** Registers the share commands; call once from the client entrypoint. */
	public static void register(ShareCoordinator coordinator) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("zea")
						.then(ClientCommands.literal("share")
								.then(ClientCommands.argument("type",
										StringArgumentType.word())
										.executes(context -> share(
												context, coordinator))))
						.then(ClientCommands.literal("import")
								.then(ClientCommands.argument("paste",
										StringArgumentType.greedyString())
										.executes(context -> beginImport(
												context, coordinator)))
								.then(ClientCommands.literal("confirm")
										.executes(context -> confirmImport(
												context, coordinator)))
								.then(ClientCommands.literal("cancel")
										.executes(context -> cancelImport(
												context, coordinator))))));
	}

	private static int share(CommandContext<FabricClientCommandSource> context, ShareCoordinator coordinator) {
		String argument = StringArgumentType.getString(context, "type");
		FabricClientCommandSource source = context.getSource();
		ShareType type;
		try {
			type = ShareType.from(argument);
		} catch (ShareException e) {
			source.sendError(Component.literal(e.getMessage()));
			return 0;
		}
		source.sendFeedback(Component.literal("Uploading your " + type.id() + "…"));
		coordinator.export(type).whenComplete((result, error) -> {
			if (result != null) {
				source.sendFeedback(Component.literal("Share link (copied to chat — click to copy): "
						+ result.key().url()));
				source.sendFeedback(Component.literal(result.note().isEmpty()
						? "Send this link to a friend; they run "
						+ "/zea import <key or link>."
						: result.note() + ". " + "Send this link to a friend; they run "
								+ "/zea import <key or link>."));
			} else {
				report(source, error);
			}
		});
		return 1;
	}

	private static int beginImport(CommandContext<FabricClientCommandSource> context,
			ShareCoordinator coordinator) {
		String paste = StringArgumentType.getString(context, "paste");
		FabricClientCommandSource source = context.getSource();
		source.sendFeedback(Component.literal("Fetching that paste…"));
		coordinator.beginImport(paste).whenComplete((preview, error) -> {
			if (preview != null) {
				source.sendFeedback(Component.literal("Import ready — " + preview.summary() + "."));
				source.sendFeedback(Component.literal(preview.application()
						+ ". Run /zea import confirm to apply,"
						+ " or /zea import cancel to discard."));
			} else {
				report(source, error);
			}
		});
		return 1;
	}

	private static int confirmImport(CommandContext<FabricClientCommandSource> context,
			ShareCoordinator coordinator) {
		FabricClientCommandSource source = context.getSource();
		ShareCoordinator.Applied applied = coordinator.confirmImport();
		if (applied == null) {
			source.sendError(Component.literal("Nothing to import — fetch a paste first with "
					+ "/zea import <key or link>."));
			return 0;
		}
		source.sendFeedback(Component.literal("Imported " + applied.type().id() + "."));
		return 1;
	}

	private static int cancelImport(CommandContext<FabricClientCommandSource> context,
			ShareCoordinator coordinator) {
		coordinator.cancelImport();
		context.getSource().sendFeedback(Component.literal("Import discarded."));
		return 1;
	}

	private static void report(FabricClientCommandSource source, Throwable error) {
		Throwable cause = error instanceof CompletionException && error.getCause() != null
				? error.getCause()
				: error;
		source.sendError(Component.literal(cause instanceof ShareException
				? cause.getMessage()
				: "Sharing failed unexpectedly — see the log for details."));
	}
}
