package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.VoteMessage;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recognizes vote notifications.
 *
 * <p>Observed shape: {@code player voted and received a reward /vote}.
 * The trailing {@code /vote} is optional and the reward phrase is parsed,
 * not hardcoded.
 */
public final class VoteClassifier implements MessageClassifier {
	private static final Pattern VOTED = Pattern.compile("^(\\w{1,16}) voted and received an? (.+?)(?: /vote)?$");

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		var matcher = VOTED.matcher(message.text());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		VoteMessage vote = new VoteMessage(matcher.group(1), matcher.group(2));
		return Optional.of(Result.of(MessageCategory.VOTE, vote));
	}

	@Override
	public String toString() {
		return "vote";
	}
}
