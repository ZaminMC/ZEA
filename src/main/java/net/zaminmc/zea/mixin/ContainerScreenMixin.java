package net.zaminmc.zea.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.buttons.ButtonRenderer;
import net.zaminmc.zea.buttons.InventoryButtons;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	private boolean zea$active(InventoryButtons buttons) {
		return buttons != null
				&& ZeaClient.lifecycle() != null
				&& ZeaClient.lifecycle().onEarthMc()
				&& ZeaClient.config() != null
				&& ZeaClient.config().inventoryButtonsEnabled();
	}
}
