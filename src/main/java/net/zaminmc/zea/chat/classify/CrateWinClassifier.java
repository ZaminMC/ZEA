package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.CrateWinMessage;
import net.zaminmc.zea.chat.model.MessageCategory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recognizes crate win notifications.
 *
 * <p>Observed shape: {@code player won 16 gold from a bruh box} with an
 * optional {@code /vote} suffix. Amounts, currencies and crate names are
 * all parsed.
 */
public final class CrateWinClassifier implements MessageClassifier {
	private static final Pattern WON = Pattern.compile("^(\\w{1,16}) won (\\d+) (\\S+) from an? (.+?)(?: /vote)?$");

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		var matcher = WON.matcher(message.text());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		CrateWinMessage win = new CrateWinMessage(
				matcher.group(1),
				Integer.parseInt(matcher.group(2)),
				matcher.group(3),
				matcher.group(4));
		return Optional.of(Result.of(MessageCategory.CRATE_WIN, win));
	}

	@Override
	public String toString() {
		return "crate-win";
	}
}
