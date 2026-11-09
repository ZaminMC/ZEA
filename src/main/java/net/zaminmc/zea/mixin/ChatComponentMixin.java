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
import net.zaminmc.zea.chat.render.ChatGeometry;
import net.zaminmc.zea.chat.render.SmoothChat;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

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
	private double getScale() {
		throw new AssertionError();
	}

	private final SmoothChat zea$smoothChat = new SmoothChat();
	/** Set while addMessageToDisplayQueue runs, to skip its internal pin scroll. */
	private boolean zea$inAddMessage;
	/** The anchor before the add, and the line count of the message being added. */
	private int zea$barBeforeAdd;
	private int zea$pendingLines;
	/** Shared before-capture for scrollChat and resetChatScroll. */
	private int zea$barBeforeScroll;

	/**
	 * Counts the wrapped lines of each added message; needed because the
	 * trimmed list is capped at 100 entries, so growth alone under-reports
	 * on busy channels.
	 */
	@WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
	private List<FormattedCharSequence> zea$countAddedLines(GuiMessage message, Font font, int maxWidth,
			Operation<List<FormattedCharSequence>> original) {
		List<FormattedCharSequence> lines = original.call(message, font, maxWidth);
		zea$pendingLines = lines.size();
		return lines;
	}

	@Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
	private void zea$captureBeforeAdd(GuiMessage message, CallbackInfo ci) {
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
		if (zea$inAddMessage || !zea$smoothChatActive()) {
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

	private int zea$durationMillis() {
		return ZeaClient.config().smoothChatDurationMillis();
	}

	private double zea$lineHeight() {
		return ChatGeometry.entryHeight(Minecraft.getInstance().options.chatLineSpacing().get());
	}
}
