package net.zaminmc.zea.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.zaminmc.zea.ZeaClient;
import net.zaminmc.zea.buttons.ButtonEditScreen;
import net.zaminmc.zea.buttons.ButtonGeometry;
import net.zaminmc.zea.buttons.InventoryButton;
import net.zaminmc.zea.buttons.InventoryButtons;
import net.zaminmc.zea.buttons.ButtonRenderer;
import net.zaminmc.zea.keybind.ZeaKeys;
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
 * popped), a hovered button suppresses the slot tooltip so the two
 * never stack, clicks run the configured command, and edit mode adds
 * dragging, an edit form and a new-button ghost. Active only while
 * connected to EarthMC — elsewhere container screens are untouched.
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
				mouseY,
				buttons.editMode(),
				buttons.drag());
	}

	@Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
	private void zea$suppressSlotTooltipOverButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			CallbackInfo ci) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (!zea$active(buttons)) {
			return;
		}
		if (buttons.editableButtonAt(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY) != null) {
			ci.cancel();
		}
	}

	/**
	 * In edit mode: Esc leaves edit mode first, and the edit keybind
	 * toggles it. Both take precedence over the screen's own Esc.
	 */
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void zea$handleEditKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (!zea$active(buttons)) {
			return;
		}
		if (buttons.editMode() && event.isEscape()) {
			buttons.toggleEditMode();
			cir.setReturnValue(true);
			return;
		}
		if (ZeaKeys.EDIT_BUTTONS.matches(event)) {
			buttons.toggleEditMode();
			cir.setReturnValue(true);
		}
	}

	/**
	 * A click on a button runs its command and is consumed, before any
	 * slot or quick-craft logic sees it. In edit mode, left-press grabs
	 * the button for dragging and right-click opens the edit form; the
	 * ghost button creates a new one. Other clicks fall through to
	 * vanilla untouched.
	 */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void zea$onButtonClick(MouseButtonEvent event, boolean doubleClick,
			CallbackInfoReturnable<Boolean> cir) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (!zea$active(buttons)) {
			return;
		}
		if (buttons.editMode()) {
			if (zea$handleEditClick(buttons, event)) {
				cir.setReturnValue(true);
			}
			return;
		}
		InventoryButton hit = buttons.buttonAt(leftPos, topPos, imageWidth, imageHeight, event.x(), event.y());
		if (hit == null) {
			return;
		}
		buttons.click(hit, System.currentTimeMillis());
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void zea$onButtonDrag(MouseButtonEvent event, double dragX, double dragY,
			CallbackInfoReturnable<Boolean> cir) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (!zea$active(buttons) || !buttons.drag().isActive()) {
			return;
		}
		buttons.drag().moveTo(event.x(), event.y());
		cir.setReturnValue(true);
	}

	/**
	 * Releasing a drag re-anchors the button and persists it. Otherwise,
	 * releasing over a button is consumed too, so vanilla never treats
	 * the button area as "outside the GUI" (which would drop a carried
	 * item). Mid-quick-craft releases pass through so the drag resolves
	 * normally.
	 */
	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void zea$onButtonRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (!zea$active(buttons)) {
			return;
		}
		if (buttons.drag().isActive()) {
			InventoryButton moved = buttons.drag().end(leftPos, topPos, imageWidth, imageHeight);
			if (moved != null) {
				buttons.upsert(moved);
			}
			cir.setReturnValue(true);
			return;
		}
		if (isQuickCrafting) {
			return;
		}
		if (buttons.editableButtonAt(leftPos, topPos, imageWidth, imageHeight, event.x(), event.y()) != null) {
			cir.setReturnValue(true);
		}
	}

	/** A closed screen must not keep a half-finished drag alive. */
	@Inject(method = "removed", at = @At("HEAD"))
	private void zea$cancelDragOnClose(CallbackInfo ci) {
		InventoryButtons buttons = ZeaClient.inventoryButtons();
		if (buttons != null) {
			buttons.drag().cancel();
		}
	}

	/**
	 * Edit-mode clicks: buttons are grabbed (left) or edited
	 * (right); the ghost button opens the edit form for a new button.
	 * Returns whether the click was consumed.
	 */
	private boolean zea$handleEditClick(InventoryButtons buttons, MouseButtonEvent event) {
		InventoryButton hit = buttons.editableButtonAt(leftPos, topPos, imageWidth, imageHeight, event.x(), event.y());
		if (hit != null) {
			if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
				Minecraft minecraft = Minecraft.getInstance();
				minecraft.gui.setScreen(new ButtonEditScreen(
								(AbstractContainerScreen<?>) (Object) this, hit, buttons));
			} else if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				int x = ButtonGeometry.x(hit, leftPos, imageWidth);
				int y = ButtonGeometry.y(hit, topPos, imageHeight);
				buttons.drag().begin(hit, x, y, event.x(), event.y());
			}
			return true;
		}
		int ghostX = ButtonGeometry.x(ButtonRenderer.GHOST_ANCHOR, ButtonRenderer.GHOST_OFFSET_X, leftPos, imageWidth);
		int ghostY = ButtonGeometry.y(ButtonRenderer.GHOST_ANCHOR, ButtonRenderer.GHOST_OFFSET_Y, topPos, 0);
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
				&& ButtonGeometry.contains(ghostX, ghostY, ButtonGeometry.SIZE, ButtonGeometry.SIZE, event.x(), event.y())) {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gui.setScreen(new ButtonEditScreen(
							(AbstractContainerScreen<?>) (Object) this,
							InventoryButton.newButton(buttons.buttons().size() + 1),
							buttons));
			return true;
		}
		return false;
	}

	private boolean zea$active(InventoryButtons buttons) {
		return buttons != null
				&& ZeaClient.lifecycle() != null
				&& ZeaClient.lifecycle().onEarthMc()
				&& ZeaClient.config() != null
				&& ZeaClient.config().inventoryButtonsEnabled();
	}
}
