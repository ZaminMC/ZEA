package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.TaggedChannelChat;

import java.util.Map;
import java.util.Optional;

/**
 * Recognizes player chat on the tagged server channels — local, trade,
 * nation and town — by their leading bracket label.
 *
 * <p>Observed shapes (text level): {@code [Local] Name: content},
 * {@code [Trade] Name: content}, {@code [Town Chat] Name: content} and
 * {@code [Nation Chat|Town] Name: content} — nation chat carries the
 * sender's town after the tag. Tags match exactly (case included) and the
 * bracket must open the message, so global affiliations such as
 * {@code [Yugoslavia|Pest]} pass through to the global classifier, which
 * runs after this one. A nation or town actually named "Local", "Trade"
 * or "Town Chat" would be claimed here first; EarthMC names are
 * geographic and none collide with these labels in practice, and the
 * tags are data in ZEA-REPO {@code datasets/channels.json} precisely so
 * a wrong tag can be corrected without a mod release. The nation form
 * without a town is unobserved and declined rather than guessed.
 */
public final class TaggedChannelChatClassifier implements MessageClassifier {
	/** Bracket labels observed on non-nation channels; exact matches. */
	private static final Map<String, MessageCategory> TAGS = Map.of(
			"Local", MessageCategory.LOCAL,
			"Trade", MessageCategory.TRADE,
			"Town Chat", MessageCategory.TOWN);

	/** The nation tag; its bracket continues with {@code |Town}. */
	private static final String NATION_TAG = "Nation Chat";

	private static final int MAX_TOWN_LENGTH = 64;

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		String text = message.text();
		if (!text.startsWith("[")) {
			return Optional.empty();
		}
		int close = text.indexOf(']');
		if (close < 0) {
			return Optional.empty();
		}
		String bracket = text.substring(1, close);

		MessageCategory category = TAGS.get(bracket);
		String tag = bracket;
		Optional<String> town = Optional.empty();
		if (category == null) {
			int split = bracket.indexOf('|');
			if (split < 0 || !bracket.substring(0, split).equals(NATION_TAG)) {
				return Optional.empty();
			}
			String townName = bracket.substring(split + 1);
			if (townName.isBlank() || townName.length() > MAX_TOWN_LENGTH) {
				return Optional.empty();
			}
			category = MessageCategory.NATION;
			tag = NATION_TAG;
			town = Optional.of(townName);
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

		return Optional.of(Result.of(category, new TaggedChannelChat(tag, town, displayName, content)));
	}

	@Override
	public String toString() {
		return "channel-tag";
	}
}
