package net.zaminmc.zea.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.buttons.ButtonRenderer;
import net.zaminmc.zea.buttons.InventoryButton;
import net.zaminmc.zea.buttons.InventoryButtons;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds ZEA's inventory buttons to container screens: the buttons draw
 * after the container contents (screen coordinates, after the pose is
 * popped), and a hovered button suppresses the slot tooltip so the two
 * never stack. Active only while connected to EarthMC — elsewhere
 * container screens are untouched.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
        @Shadow
        protected int leftPos;

        @Shadow
        protected int topPos;

        @Shadow
        @Final
        protected int imageWidth;

        @Shadow
        @Final
        protected int imageHeight;

        @Shadow
        protected boolean isQuickCrafting;

        @Inject(method = "extractContents", at = @At("TAIL"))
        private void zea$renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                        CallbackInfo ci) {
                InventoryButtons buttons = ZeaClient.inventoryButtons();
                if (!zea$active(buttons) || buttons.buttons().isEmpty()) {
                        return;
                }
                ButtonRenderer.render(
                                graphics,
                                ((AbstractContainerScreen<?>) (Object) this).getFont(),
                                buttons.buttons(),
                                leftPos,
                                topPos,
                                imageWidth,
                                imageHeight,
                                mouseX,
                                mouseY);
        }

        @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
        private void zea$suppressSlotTooltipOverButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                        CallbackInfo ci) {
                InventoryButtons buttons = ZeaClient.inventoryButtons();
                if (!zea$active(buttons)) {
                        return;
                }
                if (buttons.buttonAt(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY) != null) {
                        ci.cancel();
                }
        }

        /**
         * A click on a button runs its command and is consumed, before any
         * slot or quick-craft logic sees it. Other clicks fall through to
         * vanilla untouched.
         */
        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void zea$onButtonClick(MouseButtonEvent event, boolean doubleClick,
                        CallbackInfoReturnable<Boolean> cir) {
                InventoryButtons buttons = ZeaClient.inventoryButtons();
                if (!zea$active(buttons)) {
                        return;
                }
                InventoryButton hit = buttons.buttonAt(leftPos, topPos, imageWidth, imageHeight, event.x(), event.y());
                if (hit == null) {
                        return;
                }
                buttons.click(hit, System.currentTimeMillis());
                cir.setReturnValue(true);
        }

        /**
         * Releasing the mouse over a button is consumed too, so vanilla
         * never treats the button area as "outside the GUI" (which would
         * drop a carried item). Mid-quick-craft releases pass through so the
         * drag resolves normally.
         */
        @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
        private void zea$onButtonRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
                InventoryButtons buttons = ZeaClient.inventoryButtons();
                if (!zea$active(buttons) || isQuickCrafting) {
                        return;
                }
                if (buttons.buttonAt(leftPos, topPos, imageWidth, imageHeight, event.x(), event.y()) != null) {
                        cir.setReturnValue(true);
                }
        }

        private boolean zea$active(InventoryButtons buttons) {
                return buttons != null
                                && ZeaClient.lifecycle() != null
                                && ZeaClient.lifecycle().onEarthMc()
                                && ZeaClient.config() != null
                                && ZeaClient.config().inventoryButtonsEnabled();
        }
}
