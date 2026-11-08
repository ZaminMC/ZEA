package net.zaminmc.zea.chat.render;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.zaminmc.zea.chat.feeds.Feed;
import net.zaminmc.zea.chat.feeds.FeedSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The chat tab strip: one quiet flat tab per visible feed, drawn above the
 * chat input. A plain click selects the display feed (visual only — server
 * channels and send targets are separate states, never changed here).
 *
 * <p>Rendering is deliberately restrained: flat translucent backgrounds, no
 * borders, no icons, no animation. The selected tab is simply the most
 * solid and the only one with white text.
 */
public final class ChatTabBar extends AbstractWidget {
	/** Height of the tab strip; the chat screen positions the bar with it. */
	public static final int TAB_HEIGHT = 12;
	private static final int HORIZONTAL_PADDING = 5;
	private static final int TAB_GAP = 1;
	private static final int TEXT_Y_OFFSET = 2;
	private static final int BACKGROUND = 0x66000000;
	private static final int BACKGROUND_SELECTED = 0xAA000000;

	private final List<Feed> tabs;
	private final FeedSelection selection;
	private final Consumer<Feed> onSelect;
	private final int[] tabWidths;

	public ChatTabBar(Font font, int x, int y, int maxWidth, List<Feed> tabs,
			FeedSelection selection, Consumer<Feed> onSelect) {
		super(x, y, TAB_HEIGHT, TAB_HEIGHT, Component.literal("Chat tabs"));
		this.tabs = List.copyOf(tabs);
		this.selection = selection;
		this.onSelect = onSelect;
		this.tabWidths = new int[this.tabs.size()];
		int total = 0;
		for (int i = 0; i < this.tabs.size(); i++) {
			tabWidths[i] = font.width(this.tabs.get(i).label()) + 2 * HORIZONTAL_PADDING;
			total += tabWidths[i] + (i > 0 ? TAB_GAP : 0);
		}
		setWidth(Math.min(total, maxWidth));
		setHeight(TAB_HEIGHT);
	}

	/** The tabs this bar shows, in display order. */
	public List<Feed> tabs() {
		return tabs;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
			float partialTick) {
		Feed selected = selection.selected();
		int left = getX();
		for (int i = 0; i < tabs.size(); i++) {
			Feed tab = tabs.get(i);
			boolean isSelected = tab.equals(selected);
			extractor.fill(left, getY(), left + tabWidths[i], getY() + getHeight(),
					isSelected ? BACKGROUND_SELECTED : BACKGROUND);
			Component label = Component.literal(tab.label())
					.withStyle(isSelected ? ChatFormatting.WHITE : ChatFormatting.GRAY);
			extractor.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
					.accept(TextAlignment.LEFT, left + HORIZONTAL_PADDING, getY() + TEXT_Y_OFFSET, label);
			left += tabWidths[i] + TAB_GAP;
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		int index = tabAt(event.x());
		if (index >= 0) {
			onSelect.accept(tabs.get(index));
		}
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		// Tab selection is a quiet visual action, not a button press.
	}

	@Override
	public boolean isMouseOver(double x, double y) {
		return super.isMouseOver(x, y) && tabAt(x) >= 0;
	}

	/** Index of the tab under {@code mouseX}, or -1 in a gap / past the end. */
	private int tabAt(double mouseX) {
		if (mouseX < getX() || mouseX >= getX() + getWidth()) {
			return -1;
		}
		int left = getX();
		for (int i = 0; i < tabs.size(); i++) {
			int right = left + tabWidths[i];
			if (mouseX >= left && mouseX < right) {
				return i;
			}
			left = right + TAB_GAP;
		}
		return -1;
	}

	@Override
	protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, createNarrationMessage());
	}

	@Override
	protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
		List<String> names = new ArrayList<>();
		for (Feed tab : tabs) {
			names.add(tab.label());
		}
		return Component.translatable("gui.narrate.tabList",
				Component.literal(String.join(", ", names)));
	}
}
