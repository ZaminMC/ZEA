package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.DiscordMessage;
import net.zaminmc.zea.chat.model.MessageCategory;

import java.util.Optional;

/**
 * Recognizes messages forwarded from Discord by their {@code [Discord]}
 * prefix. Runs before the global classifier so a bracketed Discord header
 * is never mistaken for an affiliation segment.
 */
public final class DiscordClassifier implements MessageClassifier {
	private static final String PREFIX = "[Discord]";

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		String text = message.text();
		if (!text.startsWith(PREFIX)) {
			return Optional.empty();
		}
		String remainder = text.substring(PREFIX.length());
		if (remainder.startsWith(" ")) {
			remainder = remainder.substring(1);
		}
		return Optional.of(Result.of(MessageCategory.DISCORD, new DiscordMessage(remainder)));
	}

	@Override
	public String toString() {
		return "discord";
	}
}
