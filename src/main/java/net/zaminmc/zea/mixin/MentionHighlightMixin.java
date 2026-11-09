package net.zaminmc.zea.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.ARGB;
import net.zaminmc.zea.ZeaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mention highlighting: tints the background of chat lines whose message
 * contains a mention of the local player. The tint is drawn on top of
 * vanilla's per-line background, in the same rectangle and with the same
 * line alpha, so it fades together with the unfocused chat.
 *
 * <p>The wrap targets the {@code fill} call inside the line-background
 * lambda of {@code extractRenderState} (compiled as
 * {@code lambda$extractRenderState$1} in 26.3) — the one place vanilla
 * draws each visible line's background. The handler follows the
 * MixinExtras {@code @WrapOperation} contract, verified against the 0.5.5
 * injector sources: the wrapped call's receiver and arguments first, then
 * the {@link Operation}, then the enclosing lambda's arguments as a
 * trailing capture; {@code original.call} receives the receiver together
 * with the arguments. The layout is load-bearing — dev.18 crashed at
 * startup with {@code InvalidInjectionException} when this handler
 * captured the enclosing arguments before the wrapped ones and omitted
 * the receiver — and is pinned by {@code WrapOperationSignatureTest}
 * against the mapped jar. The handler keys the highlight lookup on the
 * lambda's {@link GuiMessage.Line} — the same instance the mention
 * detector marked; the receiver and the lambda's graphics parameter are
 * the same object (the call site passes {@code aload_2}). The original
 * component is never modified.
 *
 * <p>EarthMC-gated and configurable; everywhere else vanilla renders
 * untouched. Hit-testing, search and copying are unaffected: only the
 * background color changes.
 */
@Mixin(ChatComponent.class)
public abstract class MentionHighlightMixin {
	/** Gold tint over the black line background; alpha is the line's own. */
	private static final int MENTION_TINT = 0xFFAA00;
	private static final float MENTION_OPACITY = 0.30F;

	@WrapOperation(method = "lambda$extractRenderState$1",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V"))
	private static void zea$tintMentionedLine(ChatComponent.ChatGraphicsAccess graphics,
			int x0, int y0, int x1, int y1, int color,
			Operation<Void> original,
			int chatBottom, int entryHeight, ChatComponent.ChatGraphicsAccess lineGraphics,
			int maxWidth, float backgroundOpacity, GuiMessage.Line line, int lineIndex, float alpha) {
		original.call(graphics, x0, y0, x1, y1, color);
		if (zea$highlightingActive()
				&& ZeaClient.chat().mentions().highlight().isHighlighted(line.parent().content())) {
			graphics.fill(x0, y0, x1, y1, ARGB.color(alpha * MENTION_OPACITY, MENTION_TINT));
		}
	}

	private static boolean zea$highlightingActive() {
		return ZeaClient.chat() != null
				&& ZeaClient.lifecycle().onEarthMc()
				&& ZeaClient.config().mentionHighlightEnabled();
	}
}
