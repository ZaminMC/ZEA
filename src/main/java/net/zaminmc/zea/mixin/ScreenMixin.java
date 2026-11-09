package net.zaminmc.zea.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.zaminmc.zea.duck.ScreenWidgetAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Makes {@code Screen#addRenderableWidget} callable from mixins that target
 * screen subclasses. The method is protected on {@code Screen}; a mixin into
 * a subclass cannot {@code @Shadow} it, because Mixin resolves shadowed
 * members only against the target class's own declarations, not inherited
 * ones (the dev.5 chat-screen crash). This mixin targets the declaring
 * class and bridges the call through the {@link ScreenWidgetAccess} duck
 * interface instead. The interface itself lives in {@code net.zaminmc.zea.duck}:
 * Mixin owns every class in its config package outright, so a duck
 * interface there would crash the JVM the moment it resolves
 * {@code Screen}'s interfaces (the dev.16 launch crash).
 *
 * <p>The bridge delegates to the vanilla method unchanged; the JVM loads a
 * class's superclass before the class itself, so every screen subclass is
 * guaranteed to see the augmented {@code Screen}.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenWidgetAccess {
	/**
	 * The erased view of vanilla's
	 * {@code <T extends GuiEventListener & Renderable & NarratableEntry>
	 * T addRenderableWidget(T)}: shadows match on name and erased
	 * descriptor, and the erased form keeps the bridge call free of
	 * type-variable inference.
	 */
	@Shadow
	protected abstract GuiEventListener addRenderableWidget(GuiEventListener widget);

	@Override
	public GuiEventListener zea$addRenderableWidget(GuiEventListener widget) {
		return addRenderableWidget(widget);
	}
}
