package net.zaminmc.zea.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.feeds.Feed;
import net.zaminmc.zea.chat.feeds.FeedFilter;
import net.zaminmc.zea.chat.feeds.Feeds;
import net.zaminmc.zea.chat.render.ChatCopier;
import net.zaminmc.zea.chat.render.ChatSearchBar;
import net.zaminmc.zea.chat.render.ChatTabBar;
import net.zaminmc.zea.chat.render.CopyToast;
import net.zaminmc.zea.keybind.ZeaKeys;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds ZEA's chat-screen behavior: the chat tab strip, the search field
 * (Ctrl+F), and right-click message copying. Active only while connected
 * to EarthMC — elsewhere the vanilla chat is untouched.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	/** The alt modifier bit in the GLFW bitmask MouseEvent.modifiers() reports. */
	private static final int ALT_MODIFIER = 4;

	/** Vanilla draws the input background this far above the screen bottom. */
	private static final int INPUT_STRIP_HEIGHT = 14;

	@Shadow
	protected EditBox input;

	/** The search field of the currently built screen; rebuilt on resize. */
	private ChatSearchBar zea$searchBar;

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
		// addRenderableWidget is protected on Screen and inherited, so it
		// cannot be @Shadowed here — ScreenMixin exposes it instead.
		((ScreenWidgetAccess) (Object) this).zea$addRenderableWidget(tabBar);

		zea$searchBar = new ChatSearchBar(
				minecraft.font,
				2,
				screen.height - INPUT_STRIP_HEIGHT - ChatTabBar.TAB_HEIGHT - ChatSearchBar.SEARCH_HEIGHT,
				screen.width - 4);
		// Restore an open search across screen rebuilds (window resize); the
		// value is set before the responder so restoring applies no filter.
		String openQuery = filter.searchQuery();
		if (openQuery != null) {
			zea$searchBar.setValue(openQuery);
			zea$searchBar.visible = true;
		}
		zea$searchBar.setResponder(text -> {
			filter.setSearchQuery(text);
			filter.apply(minecraft.gui.hud.getChat());
		});
		((ScreenWidgetAccess) (Object) this).zea$addRenderableWidget(zea$searchBar);
	}

	/**
	 * Ctrl+F toggles the search field. Escape closes an open search before
	 * it closes the screen. While the search field is focused, confirmation
	 * returns focus to the chat input instead of sending its text.
	 */
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void zea$handleSearchKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		ChatSubsystem chat = ZeaClient.chat();
		if (chat == null || !ZeaClient.lifecycle().onEarthMc()) {
			return;
		}
		FeedFilter filter = chat.feedFilter();
		ChatScreen screen = (ChatScreen) (Object) this;
		Minecraft minecraft = Minecraft.getInstance();

		if (event.hasControlDownWithQuirk() && event.shortcutKey() == InputConstants.KEY_F
				|| ZeaKeys.SEARCH_CHAT.matches(event)) {
			if (filter.searchQuery() == null) {
				zea$openSearch(screen, filter, minecraft);
			} else {
				zea$closeSearch(screen, filter, minecraft);
			}
			cir.setReturnValue(true);
			return;
		}
		if (filter.searchQuery() != null && event.isEscape()) {
			zea$closeSearch(screen, filter, minecraft);
			cir.setReturnValue(true);
			return;
		}
		if (filter.searchQuery() != null && event.isConfirmation()
				&& zea$searchBar != null && screen.getFocused() == zea$searchBar) {
			screen.setFocused(input);
			cir.setReturnValue(true);
		}
	}

	/**
	 * Closing the chat screen closes the search too: the visible-message
	 * filter also affects the heads-up chat, which must never stay narrowed
	 * by a query the player cannot see.
	 */
	@Inject(method = "removed", at = @At("TAIL"))
	private void zea$closeSearchOnScreenClose(CallbackInfo ci) {
		ChatSubsystem chat = ZeaClient.chat();
		if (chat == null || !ZeaClient.lifecycle().onEarthMc()) {
			return;
		}
		FeedFilter filter = chat.feedFilter();
		if (filter.searchQuery() != null) {
			zea$closeSearch((ChatScreen) (Object) this, filter, Minecraft.getInstance());
		}
	}

	/**
	 * Right-click copies the message under the cursor; alt + right-click
	 * copies its formatted (legacy-coded) variant. Clicks that miss every
	 * message fall through to vanilla untouched.
	 */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void zea$copyMessageUnderCursor(MouseButtonEvent event, boolean doubled,
			CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
			return;
		}
		ChatSubsystem chat = ZeaClient.chat();
		if (chat == null || !ZeaClient.lifecycle().onEarthMc()) {
			return;
		}
		ChatScreen screen = (ChatScreen) (Object) this;
		Minecraft minecraft = Minecraft.getInstance();
		boolean formatted = (event.modifiers() & ALT_MODIFIER) != 0;
		ChatCopier.Result result = ChatCopier.copy(
				minecraft.gui.hud.getChat(),
				event.x(),
				event.y(),
				screen.height,
				formatted);
		if (result == ChatCopier.Result.NO_MESSAGE) {
			return;
		}
		CopyToast.show(minecraft, result);
		cir.setReturnValue(true);
	}

	private void zea$openSearch(ChatScreen screen, FeedFilter filter, Minecraft minecraft) {
		filter.setSearchQuery("");
		if (zea$searchBar == null) {
			return;
		}
		zea$searchBar.visible = true;
		zea$searchBar.setValue("");
		screen.setFocused(zea$searchBar);
		filter.apply(minecraft.gui.hud.getChat());
	}

	private void zea$closeSearch(ChatScreen screen, FeedFilter filter, Minecraft minecraft) {
		if (zea$searchBar != null) {
			// Clears the value before the query: the responder would re-open it.
			zea$searchBar.setValue("");
			zea$searchBar.visible = false;
		}
		filter.setSearchQuery(null);
		if (zea$searchBar != null && screen.getFocused() == zea$searchBar) {
			screen.setFocused(input);
		}
		filter.apply(minecraft.gui.hud.getChat());
	}
}
