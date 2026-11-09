package net.zaminmc.zea.mention;

import net.zaminmc.zea.chat.model.ChannelChat;
import net.zaminmc.zea.chat.model.ClassifiedMessage;
import net.zaminmc.zea.chat.model.DiscordMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.TaggedChannelChat;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Detects mentions of the local player in chat-like messages.
 *
 * <p>A mention is the local player's name appearing in the <b>content</b>
 * of a chat message someone else sent — case-insensitive, on word
 * boundaries (so {@code ZaminFan} does not ping {@code Zamin}). Only
 * chat-like categories are scanned; private messages carry their own
 * notification, and system, vote, crate and death lines are not chat.
 *
 * <p>Attribution per payload:
 *
 * <ul>
 *   <li>{@link ChannelChat} and {@link TaggedChannelChat} — speaker and
 *       content are parsed; the speaker's own echo of their message never
 *       pings.</li>
 *   <li>{@link DiscordMessage} and unclassified messages — structure is
 *       not yet verified against observed samples, so the conservative
 *       EarthMC shape {@code Name: content} is required: the word before
 *       the first {@code ": "} is the speaker, the rest is the content.
 *       Lines without that separator cannot be attributed and never
 *       ping.</li>
 * </ul>
 *
 * <p>Known limitation: EarthMC's API does not expose nicknames, so the
 * match runs against the real username. A player chatting under a
 * nickname that contains the local player's name can false-positive, and
 * nicknamed local players typing their own real name self-ping; both
 * until nickname resolution is possible (see docs/EARTHMC_API.md).
 */
public final class MentionDetector {
	/** Categories where other players can talk about you. */
	private static final Set<MessageCategory> CHAT_CATEGORIES = Set.of(
			MessageCategory.GLOBAL,
			MessageCategory.LOCAL,
			MessageCategory.TRADE,
			MessageCategory.NATION,
			MessageCategory.TOWN,
			MessageCategory.DISCORD,
			MessageCategory.UNCLASSIFIED);

	/** Separator of the unstructured chat shape {@code Name: content}. */
	private static final String SEPARATOR = ": ";

	/**
	 * Detects a mention of {@code localName} in a classified message.
	 */
	public Optional<Mention> detect(ClassifiedMessage message, String localName) {
		if (message == null || localName == null || localName.isBlank()
				|| !CHAT_CATEGORIES.contains(message.category())) {
			return Optional.empty();
		}

		if (message.payload() instanceof ChannelChat chat) {
			return forChat(message, chat.displayName(), chat.content(), localName);
		}
		if (message.payload() instanceof TaggedChannelChat chat) {
			return forChat(message, chat.displayName(), chat.content(), localName);
		}
		return forUnstructured(message, message.captured().text(), localName);
	}

	private Optional<Mention> forChat(ClassifiedMessage message, String speaker, String content, String localName) {
		if (speaker.equalsIgnoreCase(localName)) {
			return Optional.empty();
		}
		return containsWord(content, localName)
				? Optional.of(new Mention(message.captured(), speaker, content, message.category()))
				: Optional.empty();
	}

	private Optional<Mention> forUnstructured(ClassifiedMessage message, String text, String localName) {
		int separator = text.indexOf(SEPARATOR);
		if (separator <= 0) {
			return Optional.empty();
		}
		String speaker = lastWord(text.substring(0, separator));
		if (speaker.isBlank() || speaker.equalsIgnoreCase(localName)) {
			return Optional.empty();
		}
		String content = text.substring(separator + SEPARATOR.length());
		return containsWord(content, localName)
				? Optional.of(new Mention(message.captured(), speaker, content, message.category()))
				: Optional.empty();
	}

	/** Case-insensitive word-boundary match; the name charset is [A-Za-z0-9_]. */
	static boolean containsWord(String text, String name) {
		String haystack = text.toLowerCase(Locale.ROOT);
		String needle = name.toLowerCase(Locale.ROOT);
		int from = 0;
		int at;
		while ((at = haystack.indexOf(needle, from)) >= 0) {
			int end = at + needle.length();
			boolean leftOk = at == 0 || !isNameChar(text.charAt(at - 1));
			boolean rightOk = end >= text.length() || !isNameChar(text.charAt(end));
			if (leftOk && rightOk) {
				return true;
			}
			from = at + 1;
		}
		return false;
	}

	private static boolean isNameChar(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || c == '_';
	}

	private static String lastWord(String head) {
		String trimmed = head.stripTrailing();
		int start = trimmed.length();
		while (start > 0 && isNameChar(trimmed.charAt(start - 1))) {
			start--;
		}
		return trimmed.substring(start);
	}
}
