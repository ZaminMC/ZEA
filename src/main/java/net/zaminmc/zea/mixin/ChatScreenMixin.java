package net.zaminmc.zea.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ChatScreen;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.feeds.Feed;
import net.zaminmc.zea.chat.feeds.FeedFilter;
import net.zaminmc.zea.chat.feeds.Feeds;
import net.zaminmc.zea.chat.render.ChatTabBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds ZEA's chat tab strip to the chat screen. Shown only while connected
 * to EarthMC — elsewhere the vanilla chat is untouched.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	/** Vanilla draws the input background this far above the screen bottom. */
	private static final int INPUT_STRIP_HEIGHT = 14;

	@Shadow
	protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(
			T widget);

	@Inject(method = "init", at = @At("TAIL"))
	private void zea$addChatTabs(CallbackInfo ci) {
		ChatSubsystem chat = ZeaClient.chat();
		if (chat == null || !ZeaClient.lifecycle().onEarthMc()) {
			return;
		}
		ChatScreen screen = (ChatScreen) (Object) this;
		Minecraft minecraft = Minecraft.getInstance();
		FeedFilter filter = chat.feedFilter();

		ChatTabBar tabBar = new ChatTabBar(
				minecraft.font,
				2,
				screen.height - INPUT_STRIP_HEIGHT - ChatTabBar.TAB_HEIGHT,
				screen.width - 4,
				Feeds.visibleTabs(ZeaClient.config().enabledExtraFeedIds()),
				chat.feedSelection(),
				feed -> {
					chat.feedSelection().select(feed);
					filter.apply(minecraft.gui.hud.getChat());
				});
		addRenderableWidget(tabBar);
	}
}
