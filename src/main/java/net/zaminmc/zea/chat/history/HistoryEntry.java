package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.util.LegacyText;

import java.time.Instant;

/**
 * One chat message as stored in history: the classification plus the
 * message re-expressed as plain and legacy-formatted text.
 *
 * <p>History is deliberately a <b>projection</b>, not a container for
 * the live {@code Component}: components are runtime objects tied to the
 * running session, while history must survive restarts in a plain JSON
 * file. The plain text is what search matches; the legacy-formatted text
 * is what display re-uses (the font engine interprets {@code §} codes
 * inside literal text). Hover and click events are not preserved — they
 * are ephemeral server constructs and meaningless days later.
 *
 * @param receivedAt    when the message was captured
 * @param category      classification; {@code UNCLASSIFIED} for unknowns
 * @param plainText     flattened text, no formatting codes
 * @param formattedText text with legacy {@code §} codes re-expressed
 */
public record HistoryEntry(Instant receivedAt, MessageCategory category, String plainText,
		String formattedText) {

	public HistoryEntry {
		if (receivedAt == null || category == null || plainText == null || formattedText == null) {
			throw new NullPointerException("history entry fields must not be null");
		}
	}

	/**
	 * Projects a classified message into its history form. Formatting is
	 * computed here, once, on the client thread — the recorded entry is
	 * then immutable and safe to serialize off-thread.
	 */
	public static HistoryEntry of(ClassifiedMessage message) {
		return new HistoryEntry(
				message.captured().receivedAt(),
				message.category(),
				message.captured().text(),
				LegacyText.format(message.captured().original()));
	}
}
