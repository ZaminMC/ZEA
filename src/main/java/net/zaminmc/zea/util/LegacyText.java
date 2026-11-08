package net.zaminmc.zea.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serializes components back to legacy {@code §}-coded text — Minecraft
 * 26.3 ships no such serializer, and the formatted-copy feature needs the
 * codes preserved exactly as the server sent them.
 *
 * <p>Emission rule, mirroring how EarthMC itself emits chat: whenever a
 * run's effective style differs from the previous run's, the run is
 * prefixed with {@code §r} followed by its complete code set. This never
 * leaks a color or flag across runs and reproduces the server's observed
 * segment style. Colors outside the legacy 16 have no code and are
 * skipped.
 */
public final class LegacyText {
	private static final Map<Integer, ChatFormatting> LEGACY_BY_RGB = new HashMap<>();

	static {
		for (ChatFormatting formatting : ChatFormatting.values()) {
			TextColor color = TextColor.fromLegacyFormat(formatting);
			if (color != null) {
				LEGACY_BY_RGB.put(color.getValue(), formatting);
			}
		}
	}

	private LegacyText() {
	}

	/** The flattened, code-free text of the component. */
	public static String plain(Component component) {
		return component.getString();
	}

	/** The component with formatting re-expressed as legacy codes. */
	public static String format(Component component) {
		RunState state = new RunState();
		component.visit((style, text) -> {
			if (!text.isEmpty()) {
				state.appendRun(style, text);
			}
			return Optional.empty();
		}, Style.EMPTY);
		return state.out.toString();
	}

	/** The style carried across runs while serializing one component. */
	private static final class RunState {
		final StringBuilder out = new StringBuilder();
		ActiveStyle current = ActiveStyle.EMPTY;

		void appendRun(Style style, String text) {
			ActiveStyle next = ActiveStyle.of(style);
			if (!next.equals(current)) {
				if (!current.equals(ActiveStyle.EMPTY)) {
					out.append(ChatFormatting.RESET.toString());
				}
				next.emitInto(out);
			}
			out.append(text);
			current = next;
		}
	}

	private record ActiveStyle(ChatFormatting color, boolean bold, boolean italic, boolean underline,
			boolean strikethrough, boolean obfuscated) {
		static final ActiveStyle EMPTY = new ActiveStyle(null, false, false, false, false, false);

		static ActiveStyle of(Style style) {
			return new ActiveStyle(legacyColor(style.getColor()), style.isBold(), style.isItalic(),
					style.isUnderlined(), style.isStrikethrough(), style.isObfuscated());
		}

		/** All codes of this style, in a stable order (color first). */
		void emitInto(StringBuilder out) {
			if (color != null) {
				out.append(color.toString());
			}
			if (bold) {
				out.append(ChatFormatting.BOLD.toString());
			}
			if (italic) {
				out.append(ChatFormatting.ITALIC.toString());
			}
			if (underline) {
				out.append(ChatFormatting.UNDERLINE.toString());
			}
			if (strikethrough) {
				out.append(ChatFormatting.STRIKETHROUGH.toString());
			}
			if (obfuscated) {
				out.append(ChatFormatting.OBFUSCATED.toString());
			}
		}
	}

	private static ChatFormatting legacyColor(TextColor color) {
		return color == null ? null : LEGACY_BY_RGB.get(color.getValue());
	}
}
