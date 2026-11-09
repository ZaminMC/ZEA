package net.zaminmc.zea.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * The chat search field: a borderless, flat-background edit box shown
 * above the tab strip while a search is open (Ctrl+F). Text editing,
 * cursor and selection are all vanilla {@link EditBox} behavior — this
 * class only adds the flat background the chat aesthetic needs and
 * fixes the height to the chat widget grid.
 */
public final class ChatSearchBar extends EditBox {
	/** Height of the search row; the chat screen positions the bar with it. */
	public static final int SEARCH_HEIGHT = 12;
	private static final int BACKGROUND = 0x66000000;

	public ChatSearchBar(Font font, int x, int y, int width) {
		super(font, x, y, width, SEARCH_HEIGHT, Component.translatable("zea.chat.search"));
		setBordered(false);
		setHeight(SEARCH_HEIGHT);
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
			float partialTick) {
		if (visible) {
			extractor.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), BACKGROUND);
		}
		super.extractWidgetRenderState(extractor, mouseX, mouseY, partialTick);
	}
}
