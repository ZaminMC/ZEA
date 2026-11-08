package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.WritingChannelReport;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Recognizes the server's writing-channel report:
 * {@code You are currently in global (write) and 5 other channels}.
 *
 * <p>Runs before every generic classifier — this response is the only
 * authoritative source for the server-side writing channel (Stage 3).
 */
public final class WritingChannelReportClassifier implements MessageClassifier {
	private static final Pattern REPORT = Pattern.compile(
			"^You are currently in (\\S+) \\(write\\) and (\\d+) other channels$");

	@Override
	public Optional<Result> tryClassify(CapturedMessage message) {
		var matcher = REPORT.matcher(message.text());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		WritingChannelReport report = new WritingChannelReport(matcher.group(1), Integer.parseInt(matcher.group(2)));
		return Optional.of(Result.of(MessageCategory.SYSTEM, report));
	}

	@Override
	public String toString() {
		return "writing-channel-report";
	}
}
