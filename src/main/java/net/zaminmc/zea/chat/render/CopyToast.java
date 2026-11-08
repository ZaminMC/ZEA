package net.zaminmc.zea.chat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * The "message copied" confirmation toast. Uses the vanilla system toast
 * with ZEA's own token so it never replaces or queues behind a vanilla
 * notification.
 */
public final class CopyToast {
	/** ZEA's dedicated toast token; distinct from every vanilla id. */
	private static final SystemToast.SystemToastId TOKEN = new SystemToast.SystemToastId();

	private CopyToast() {
	}

	/** Shows the confirmation for the kind of copy that just happened. */
	public static void show(Minecraft minecraft, ChatCopier.Result result) {
		String key = switch (result) {
			case PLAIN -> "zea.chat.copy.plain";
			case FORMATTED -> "zea.chat.copy.formatted";
			case NO_MESSAGE -> null;
		};
		if (key == null) {
			return;
		}
		SystemToast.addOrUpdate(
				minecraft.gui.toastManager(),
				TOKEN,
				Component.translatable("zea.chat.copy.title"),
				Component.translatable(key));
	}
}
