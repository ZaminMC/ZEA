package net.zaminmc.zea.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Read access to the chat line state ZEA's hit-testing needs: the trimmed
 * (wrapped) lines, the scroll position, and the private scale/width
 * computations the vanilla renderer uses.
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
	@Accessor("trimmedMessages")
	List<GuiMessage.Line> zea$getTrimmedMessages();

	@Accessor("chatScrollbarPos")
	int zea$getChatScrollbarPos();

	@Invoker("getScale")
	double zea$invokeGetScale();

	@Invoker("getWidth")
	int zea$invokeGetWidth();
}
