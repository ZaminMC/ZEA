package net.zaminmc.zea.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Duck interface {@link ScreenMixin} adds to every screen: it exposes
 * {@code Screen#addRenderableWidget} to code that holds a screen but is not
 * a screen subclass — above all, mixins that target one.
 *
 * <p>The method is protected and declared on {@code Screen} itself, so a
 * mixin into a screen subclass (the chat screen, say) cannot shadow it:
 * Mixin resolves {@code @Shadow} and injection targets only against the
 * target class's own declared members, never inherited ones. Casting the
 * screen here reaches the vanilla method unchanged, with whatever
 * registration semantics the current version has.
 */
public interface ScreenWidgetAccess {
	/**
	 * Registers a widget with the screen — renderable, event listener and
	 * narratable — exactly like {@code Screen#addRenderableWidget}.
	 *
	 * @param widget the widget to add
	 * @return the widget, typed as its listener view
	 */
	GuiEventListener zea$addRenderableWidget(GuiEventListener widget);
}
