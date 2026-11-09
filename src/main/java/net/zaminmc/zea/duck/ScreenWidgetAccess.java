package net.zaminmc.zea.duck;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Duck interface {@link net.zaminmc.zea.mixin.ScreenMixin} adds to every
 * screen: it exposes {@code Screen#addRenderableWidget} to code that holds
 * a screen but is not a screen subclass — above all, mixins that target one.
 *
 * <p>The method is protected and declared on {@code Screen} itself, so a
 * mixin into a screen subclass (the chat screen, say) cannot shadow it:
 * Mixin resolves {@code @Shadow} and injection targets only against the
 * target class's own declared members, never inherited ones. Casting the
 * screen here reaches the vanilla method unchanged, with whatever
 * registration semantics the current version has.
 *
 * <p><b>Must live outside the mixin package.</b> Mixin owns every class
 * under {@code net.zaminmc.zea.mixin.*} and refuses to load any of them
 * as a normal class — only registered accessor mixins get the pass-through
 * carve-out. This interface is merged into {@code Screen}'s interface
 * list, which the JVM resolves eagerly when defining the class, so a
 * mixin-package address here crashed every launch until dev.16 (see
 * WORKLOG). {@code MixinPackageHygieneTest} keeps it out.
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
