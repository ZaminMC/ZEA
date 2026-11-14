package net.zaminmc.zea.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.chat.ChatSubsystem;
import net.zaminmc.zea.chat.feeds.FeedFilter;
import net.zaminmc.zea.chat.render.ChatGeometry;
import net.zaminmc.zea.chat.render.SmoothChat;
import net.zaminmc.zea.chat.render.SplitLineCache;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

/**
 * Smooth chat: whenever the chat's scroll anchor jumps — a new message
 * arriving at the bottom, mouse-wheel or page scrolling, the scroll reset
 * when the chat screen closes — the visual position keeps continuity and
 * glides to the new anchor over the configured duration (quartic
 * ease-out).
 *
 * <p>Trigger hooks record the anchor before and after each jump; the
 * render hook translates the chat pose by the eased remainder. All hooks
 * are EarthMC-gated and configurable, so everywhere else vanilla renders
 * untouched. Click and hover hit regions are computed from the settled
 * anchor, so they can lag the visuals by the (sub-second) animation; in
 * practice clicks land after the glide finishes.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Shadow
	private int chatScrollbarPos;

	@Shadow
	private boolean newMessageSinceScroll;

	@Shadow
	private double getScale() {
		throw new AssertionError();
	}

	@Shadow
	private void addMessageToQueue(GuiMessage message) {
		throw new AssertionError();
	}

	@Shadow
	public void scrollChat(int direction) {
		throw new AssertionError();
	}

	private final SmoothChat zea$smoothChat = new SmoothChat();
	/** Memoized line wraps for the display rebuilds; see {@link SplitLineCache}. */
	private final SplitLineCache zea$splitLines = new SplitLineCache(2048);
	/** Set while addMessageToDisplayQueue runs, to skip its internal pin scroll. */
	private boolean zea$inAddMessage;
	/** Set while refreshTrimmedMessages rebuilds the display list. */
	private boolean zea$inRefresh;
	/** The anchor before the add, and the line count of the message being added. */
	private int zea$barBeforeAdd;
	private int zea$pendingLines;
	/** Shared before-capture for scrollChat and resetChatScroll. */
	private int zea$barBeforeScroll;
	/** The scroll anchor refreshTrimmedMessages started from. */
	private int zea$scrollBeforeRefresh;

	/**
	 * Counts the wrapped lines of each added message; needed because the
	 * trimmed list is capped at 100 entries, so growth alone under-reports
	 * on busy channels. On EarthMC the wrap itself is memoized — a
	 * rebuild re-adds the whole history, and re-wrapping every message
	 * per tab switch is the live-reported lag — while everywhere else
	 * vanilla's compute-per-call behavior is kept byte-for-byte.
	 */
	@WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
	private List<FormattedCharSequence> zea$countAddedLines(GuiMessage message, Font font, int maxWidth,
			Operation<List<FormattedCharSequence>> original) {
		List<FormattedCharSequence> lines;
		if (zea$onEarthMc()) {
			lines = zea$splitLines.cached(message, font, maxWidth);
			if (lines == null) {
				lines = original.call(message, font, maxWidth);
				zea$splitLines.remember(message, font, maxWidth, lines);
			}
		} else {
			lines = original.call(message, font, maxWidth);
		}
		zea$pendingLines = lines.size();
		return lines;
	}

	@Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
	private void zea$captureBeforeAdd(GuiMessage message, CallbackInfo ci) {
		if (zea$inRefresh) {
			return;
		}
		if (zea$smoothChatActive()) {
			zea$inAddMessage = true;
			zea$barBeforeAdd = chatScrollbarPos;
		}
	}

	/**
	 * A message that arrived while the chat sat at the bottom pushes every
	 * line up by its wrapped line count — animate that slide. While
	 * scrolled up, vanilla pins the view via an internal scrollChat call;
	 * nothing moves visually, so there is nothing to animate.
	 */
	@Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
	private void zea$animateNewMessage(GuiMessage message, CallbackInfo ci) {
		zea$inAddMessage = false;
		if (zea$inRefresh) {
			// A rebuild re-adds every message; none of them is
			// "new", so none of them glides.
			zea$pendingLines = 0;
			return;
		}
		if (zea$smoothChatActive() && zea$barBeforeAdd == 0 && chatScrollbarPos == 0 && zea$pendingLines > 0) {
			zea$smoothChat.shift(zea$pendingLines * zea$lineHeight(),
					System.currentTimeMillis(), zea$durationMillis());
		}
		zea$pendingLines = 0;
	}

	@Inject(method = "scrollChat", at = @At("HEAD"))
	private void zea$captureBeforeScroll(int notches, CallbackInfo ci) {
		zea$barBeforeScroll = chatScrollbarPos;
	}

	@Inject(method = "scrollChat", at = @At("TAIL"))
	private void zea$animateScroll(int notches, CallbackInfo ci) {
		if (zea$inAddMessage || zea$inRefresh || !zea$smoothChatActive()) {
			return;
		}
		int lines = zea$barBeforeScroll - chatScrollbarPos;
		if (lines != 0) {
			zea$smoothChat.shift(lines * zea$lineHeight(),
					System.currentTimeMillis(), zea$durationMillis());
		}
	}

	@Inject(method = "resetChatScroll", at = @At("HEAD"))
	private void zea$captureBeforeReset(CallbackInfo ci) {
		// resetChatScroll never runs inside a refresh, so this is a
		// safe point to clear a refresh flag an exception left set
		// (a third-party filter that throws inside the rebuild).
		zea$inRefresh = false;
		zea$barBeforeScroll = chatScrollbarPos;
	}

	/** Closing the chat screen drops the scroll back to the bottom; glide there. */
	@Inject(method = "resetChatScroll", at = @At("TAIL"))
	private void zea$animateReset(CallbackInfo ci) {
		if (!zea$smoothChatActive()) {
			return;
		}
		int lines = zea$barBeforeScroll - chatScrollbarPos;
		if (lines != 0) {
			zea$smoothChat.shift(lines * zea$lineHeight(),
					System.currentTimeMillis(), zea$durationMillis());
		}
	}

	/**
	 * Vanilla applies the visible-message filter as an ingest gate:
	 * {@code addMessage} returns before its store calls when the filter
	 * rejects, so a message hidden by the selected feed would vanish
	 * from the chat entirely — {@code allMessages} never receives it,
	 * and switching back to All could not restore it (verified against
	 * the 26.3 bytecode). When the installed filter is ZEA's, store the
	 * rejected message into {@code allMessages} before vanilla drops
	 * it: hidden from the display list now, retained for every other
	 * feed and the All aggregate. Any other filter (vanilla's own
	 * chat-trust filter, other mods') keeps vanilla's drop semantics —
	 * the wrap is then a pure passthrough.
	 *
	 * <p>Messages passing a ZEA filter are also classified right here,
	 * once, as they arrive: the feed predicate classifies on cache miss,
	 * so a message that skips this step (history restored by another
	 * mod, or accepted by the always-true All filter) would otherwise
	 * run the whole pipeline inside the next tab switch's rebuild —
	 * repeatedly, once per switch, until the cache sees it. With the
	 * wrap classifying at ingest, every later rebuild is a lookup.
	 */
	@WrapOperation(method = "addMessage", at = @At(value = "INVOKE",
			target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
	private boolean zea$retainMessagesHiddenByZeaFilters(Predicate<GuiMessage> filter, Object message,
			Operation<Boolean> original) {
		boolean shown = original.call(filter, message);
		if (filter instanceof FeedFilter.Installed) {
			ChatSubsystem chat = ZeaClient.chat();
			if (chat != null) {
				chat.classifications().classificationOf(((GuiMessage) message).content());
			}
			if (!shown) {
				addMessageToQueue((GuiMessage) message);
			}
		}
		return shown;
	}

	/**
	 * A bulk rebuild is not an append: the per-line scroll compensation
	 * {@code addMessageToDisplayQueue} applies (one notch per line while
	 * the chat is open and scrolled up) exists to keep single new
	 * messages steady, and across a whole rebuild it inflates the anchor
	 * until it clamps against the new list — the view jumps toward the
	 * oldest messages on every tab switch. Remember the anchor before
	 * the rebuild and restore it after, letting vanilla's own
	 * {@code scrollChat(0)} re-clamp it to the new list. The smooth-chat
	 * hooks check {@code zea$inRefresh} so a rebuilt message never
	 * counts as new or as scrolling. Off EarthMC this is inert: vanilla's
	 * own filter toggles keep their behavior byte-for-byte.
	 */
	@Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
	private void zea$rememberScrollBeforeRefresh(CallbackInfo ci) {
		zea$inRefresh = true;
		zea$scrollBeforeRefresh = chatScrollbarPos;
	}

	@Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
	private void zea$restoreScrollAfterRefresh(CallbackInfo ci) {
		zea$inRefresh = false;
		if (!zea$onEarthMc()) {
			return;
		}
		chatScrollbarPos = zea$scrollBeforeRefresh;
		newMessageSinceScroll = false;
		scrollChat(0);
	}

	/**
	 * The chat forgetting its messages forgets their wraps with them:
	 * keeping the memo alive past {@code clearMessages} would pin the
	 * components of a dead conversation for no rebuild that can ever
	 * ask for them again.
	 */
	@Inject(method = "clearMessages", at = @At("TAIL"))
	private void zea$forgetSplitLines(CallbackInfo ci) {
		zea$splitLines.clear();
	}

	/**
	 * Applies the eased offset to the chat's pushed pose, in screen
	 * pixels: the translate happens before vanilla applies the chat
	 * scale, and chat units times the scale are gui-scaled pixels.
	 */
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
			at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;"))
	private Matrix3x2fStack zea$applySmoothChatOffset(Matrix3x2fStack stack,
			Operation<Matrix3x2fStack> original) {
		Matrix3x2fStack pushed = original.call(stack);
		if (zea$smoothChatActive()) {
			double units = zea$smoothChat.offsetAt(System.currentTimeMillis(), zea$durationMillis());
			if (units != 0) {
				pushed.translate(0.0f, (float) (units * getScale()));
			}
		}
		return pushed;
	}

	private boolean zea$smoothChatActive() {
		return ZeaClient.chat() != null
				&& ZeaClient.lifecycle().onEarthMc()
				&& ZeaClient.config().smoothChatEnabled();
	}

	private boolean zea$onEarthMc() {
		return ZeaClient.chat() != null && ZeaClient.lifecycle().onEarthMc();
	}

	private int zea$durationMillis() {
		return ZeaClient.config().smoothChatDurationMillis();
	}

	private double zea$lineHeight() {
		return ChatGeometry.entryHeight(Minecraft.getInstance().options.chatLineSpacing().get());
	}
}
