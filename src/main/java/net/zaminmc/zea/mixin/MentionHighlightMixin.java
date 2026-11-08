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
 * draws each visible line's background. The handler captures the lambda's
 * parameters, including the {@link GuiMessage.Line} being drawn, and keys
 * the highlight lookup on the line's message component — the same
 * instance the mention detector marked. The original component is never
 * modified.
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
        private static void zea$tintMentionedLine(int chatBottom, int entryHeight,
                        ChatComponent.ChatGraphicsAccess graphics, int maxWidth, float backgroundOpacity,
                        GuiMessage.Line line, int lineIndex, float alpha,
                        int x0, int y0, int x1, int y1, int color,
                        Operation<Void> original) {
                original.call(x0, y0, x1, y1, color);
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
