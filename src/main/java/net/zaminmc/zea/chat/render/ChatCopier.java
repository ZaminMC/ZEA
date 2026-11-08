package net.zaminmc.zea.chat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.zaminmc.zea.util.LegacyText;

/**
 * The chat message copying policy: hit-test the cursor, pick the plain or
 * formatted serialization, write the clipboard. The chat screen mixin is a
 * thin caller of this class so the policy stays in one place.
 */
public final class ChatCopier {
	/** What a copy attempt did. */
	public enum Result {
		/** Plain text was placed on the clipboard. */
		PLAIN,
		/** Legacy-formatted text was placed on the clipboard. */
		FORMATTED,
		/** The cursor was not over a chat message; nothing happened. */
		NO_MESSAGE
	}

	private ChatCopier() {
	}

	/**
	 * The text a copy of this message would place on the clipboard: the
	 * flattened text, or the legacy-coded serialization when formatting is
	 * requested.
	 */
	public static String textFor(GuiMessage message, boolean formatted) {
		return formatted ? LegacyText.format(message.content()) : LegacyText.plain(message.content());
	}

	/**
	 * Copies the message under the cursor, if any.
	 *
	 * @param chat the live chat component the player is looking at
	 * @param mouseX the cursor's gui-scaled x position
	 * @param mouseY the cursor's gui-scaled y position
	 * @param guiHeight the gui-scaled screen height
	 * @param formatted whether the alt-modified (formatted) variant is wanted
	 * @return what happened; {@link Result#NO_MESSAGE} leaves the click for
	 *         vanilla to handle
	 */
	public static Result copy(ChatComponent chat, double mouseX, double mouseY, int guiHeight, boolean formatted) {
		GuiMessage message = ChatHitTester.messageAt(chat, mouseX, mouseY, guiHeight);
		if (message == null) {
			return Result.NO_MESSAGE;
		}
		Minecraft.getInstance().keyboardHandler.setClipboard(textFor(message, formatted));
		return formatted ? Result.FORMATTED : Result.PLAIN;
	}
}
