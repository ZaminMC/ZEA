package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.DeathMessage;
import net.zaminmc.zea.chat.model.MessageCategory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recognizes death notifications.
 *
 * <p>Observed shape: {@code ☠ victim was killed by killer}. Other death
 * verb forms are not guessed — unrecognized variants stay unclassified
 * until observed. Combat details live in the original component's hover
 * event, which is always retained.
 */
public final class DeathClassifier implements MessageClassifier {
	private static final Pattern KILLED_BY = Pattern.compile("^☠ (\\S+) was killed by (\\S+)$");

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		var matcher = KILLED_BY.matcher(message.text());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		DeathMessage death = new DeathMessage(matcher.group(1), matcher.group(2));
		return Optional.of(Result.of(MessageCategory.DEATH, death));
	}

	@Override
	public String toString() {
		return "death";
	}
}
