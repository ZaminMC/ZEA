package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.SystemMessage;

import java.util.Optional;

/**
 * Recognizes server system messages prefixed with {@code EarthMC »},
 * including AFK notifications.
 */
public final class SystemClassifier implements MessageClassifier {
	private static final String PREFIX = "EarthMC » ";

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		String text = message.text();
		if (!text.startsWith(PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(Result.of(MessageCategory.SYSTEM, new SystemMessage(text.substring(PREFIX.length()))));
	}

	@Override
	public String toString() {
		return "system";
	}
}
