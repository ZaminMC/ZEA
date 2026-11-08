package net.zaminmc.zea.chat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.zaminmc.zea.mixin.ChatComponentAccessor;

import java.util.List;

/**
 * Finds the chat message under the cursor, using {@link ChatGeometry}'s
 * mirror of the vanilla line layout and accessor reads of the live chat
 * state. Screen coordinates are the gui-scaled ones the screen receives.
 */
public final class ChatHitTester {
	private ChatHitTester() {
	}

	/** The whole message under the cursor, or {@code null} when not over one. */
	public static GuiMessage messageAt(ChatComponent chat, double mouseX, double mouseY, int guiHeight) {
		ChatComponentAccessor access = (ChatComponentAccessor) chat;
		double scale = access.zea$invokeGetScale();
		if (scale <= 0) {
			scale = 1;
		}
		double spacing = Minecraft.getInstance().options.chatLineSpacing().get();
		int chatBottom = ChatGeometry.chatBottom(guiHeight, scale);
		int chatWidth = (int) Math.ceil(access.zea$invokeGetWidth() / scale);
		int slot = ChatGeometry.lineSlotAt(mouseX / scale, mouseY / scale, chatBottom, chatWidth,
				ChatGeometry.entryHeight(spacing), ChatGeometry.entryBottomToMessageY(spacing));
		if (slot < 0) {
			return null;
		}

		List<GuiMessage.Line> lines = access.zea$getTrimmedMessages();
		int scroll = access.zea$getChatScrollbarPos();
		// Mirrors forEachLine: slots 0..min(size - scroll, linesPerPage) - 1 are drawn.
		int drawn = Math.min(lines.size() - scroll, chat.getLinesPerPage());
		if (slot >= drawn) {
			return null;
		}
		int listIndex = slot + scroll;
		if (listIndex < 0 || listIndex >= lines.size()) {
			return null;
		}
		return lines.get(listIndex).parent();
	}
}
