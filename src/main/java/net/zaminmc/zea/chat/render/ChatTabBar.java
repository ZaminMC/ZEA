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
import java.util.function.Predicate;

/**
 * The chat tab strip: one quiet flat tab per visible feed, drawn above the
 * chat input. A plain click selects the display feed (visual only — server
 * channels are a separate state, never changed here). Shift-click toggles
 * the feed's channel as a multi-send target (All and the event feeds have
 * no channel and the toggle explains that); targeting is also a separate
 * state, reflected here as a thin marker strip and aqua label.
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
	/** The GLFW modifier bit for shift in MouseEvent.modifiers(). */
	private static final int SHIFT_MODIFIER = 1;
	/** Height of the marker strip under a multi-send target tab. */
	private static final int TARGET_MARKER_HEIGHT = 1;
	private static final int TARGET_MARKER = 0xFF3FF5E0;

	private final List<Feed> tabs;
	private final FeedSelection selection;
	private final Consumer<Feed> onSelect;
	private final Consumer<Feed> onToggleTarget;
	private final Predicate<Feed> isTargeted;
	private final int[] tabWidths;

	public ChatTabBar(Font font, int x, int y, int maxWidth, List<Feed> tabs,
			FeedSelection selection, Predicate<Feed> isTargeted, Consumer<Feed> onSelect,
			Consumer<Feed> onToggleTarget) {
		super(x, y, TAB_HEIGHT, TAB_HEIGHT, Component.literal("Chat tabs"));
		this.tabs = List.copyOf(tabs);
		this.selection = selection;
		this.isTargeted = isTargeted;
		this.onSelect = onSelect;
		this.onToggleTarget = onToggleTarget;
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
			boolean targeted = isTargeted.test(tab);
			extractor.fill(left, getY(), left + tabWidths[i], getY() + getHeight(),
					isSelected ? BACKGROUND_SELECTED : BACKGROUND);
			if (targeted) {
				extractor.fill(left, getY() + getHeight() - TARGET_MARKER_HEIGHT,
						left + tabWidths[i], getY() + getHeight(), TARGET_MARKER);
			}
			ChatFormatting labelColor = isSelected ? ChatFormatting.WHITE
					: targeted ? ChatFormatting.AQUA : ChatFormatting.GRAY;
			Component label = Component.literal(tab.label()).withStyle(labelColor);
			extractor.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE)
					.accept(TextAlignment.LEFT, left + HORIZONTAL_PADDING, getY() + TEXT_Y_OFFSET, label);
			left += tabWidths[i] + TAB_GAP;
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		int index = tabAt(event.x());
		if (index < 0) {
			return;
		}
		Feed tab = tabs.get(index);
		if ((event.modifiers() & SHIFT_MODIFIER) != 0) {
			onToggleTarget.accept(tab);
		} else {
			onSelect.accept(tab);
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
			String name = tab.label();
			if (isTargeted.test(tab)) {
				name += " " + Component.translatable("zea.chat.multisend.narration_target").getString();
			}
			names.add(name);
		}
		return Component.translatable("gui.narrate.tabList",
				Component.literal(String.join(", ", names)));
	}
}
