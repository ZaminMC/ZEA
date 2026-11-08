package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.Affiliation;
import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ChannelChat;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;

import java.util.Optional;

/**
 * Recognizes player chat on the global channel.
 *
 * <p>Observed shape (text level): {@code <badges> [Nation|Town] Name:
 * content}, where badges may be absent and the affiliation may be a single
 * segment. Parsing is anchored — affiliation bracket first, then display
 * name up to the first {@code ": "}. A bracket-less message is declined:
 * "name: content" alone is too weak an anchor to claim, so such messages
 * stay unclassified until observed fixtures verify their shape.
 */
public final class GlobalChatClassifier implements MessageClassifier {
	private static final int MAX_AFFILIATION_LENGTH = 64;

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		String text = message.text();

		int bracket = text.indexOf('[');
		if (bracket < 0) {
			return Optional.empty();
		}
		int close = text.indexOf(']', bracket);
		if (close < 0) {
			return Optional.empty();
		}
		String affiliationRaw = text.substring(bracket + 1, close);
		if (affiliationRaw.isBlank() || affiliationRaw.length() > MAX_AFFILIATION_LENGTH) {
			return Optional.empty();
		}

		String rest = text.substring(close + 1);
		if (!rest.startsWith(" ")) {
			return Optional.empty();
		}
		rest = rest.substring(1);
		int colon = rest.indexOf(": ");
		if (colon <= 0) {
			return Optional.empty();
		}
		String displayName = rest.substring(0, colon);
		if (displayName.isBlank() || displayName.indexOf('[') >= 0 || displayName.indexOf(']') >= 0) {
			return Optional.empty();
		}
		String content = rest.substring(colon + 2);

		ChannelChat chat = new ChannelChat(
				BadgeExtractor.extract(message.original(), bracket),
				Affiliation.parse(affiliationRaw),
				displayName,
				content);
		return Optional.of(Result.of(MessageCategory.GLOBAL, chat));
	}

	@Override
	public String toString() {
		return "global";
	}
}
