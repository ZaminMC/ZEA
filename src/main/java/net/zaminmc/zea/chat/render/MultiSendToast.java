package net.zaminmc.zea.chat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.feeds.MultiSendInteraction;

/**
 * The multi-send feedback toasts. Every multi-send outcome is a toast —
 * never a chat message — because the chat HUD is feed-filtered: a client
 * system message would be invisible exactly when a channel tab is
 * selected, which is the multi-send situation. Uses the vanilla system
 * toast with ZEA's own token, like {@link CopyToast}.
 */
public final class MultiSendToast {
	/** ZEA's dedicated toast token; distinct from every vanilla id. */
	private static final SystemToast.SystemToastId TOKEN = new SystemToast.SystemToastId();

	private MultiSendToast() {
	}

	/** Reports what a shift-click on a tab did (or why it did nothing). */
	public static void showToggle(Minecraft minecraft, MultiSendInteraction.ToggleOutcome outcome, String label) {
		String key = switch (outcome) {
			case ADDED -> "zea.chat.multisend.added";
			case REMOVED -> "zea.chat.multisend.removed";
			case NO_CHANNEL -> "zea.chat.multisend.no_channel";
			case DISABLED -> "zea.chat.multisend.disabled";
		};
		SystemToast.addOrUpdate(
				minecraft.gui.toastManager(),
				TOKEN,
				Component.translatable("zea.chat.multisend.title"),
				Component.translatable(key, label));
	}

	/** Confirms an accepted batch: how many messages go where. */
	public static void showAccepted(Minecraft minecraft, int count, String destinations) {
		String messages = count + (count == 1 ? " message" : " messages");
		SystemToast.addOrUpdate(
				minecraft.gui.toastManager(),
				TOKEN,
				Component.translatable("zea.chat.multisend.title"),
				Component.translatable("zea.chat.multisend.accepted", messages, destinations));
	}

	/** Reports a failed dispatch and how many sends were cancelled. */
	public static void showDispatchFailed(Minecraft minecraft, String command, int cancelled) {
		String remaining = cancelled + (cancelled == 1 ? " message" : " messages");
		SystemToast.addOrUpdate(
				minecraft.gui.toastManager(),
				TOKEN,
				Component.translatable("zea.chat.multisend.title"),
				Component.translatable("zea.chat.multisend.failed", command, remaining));
	}
}
