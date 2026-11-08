package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.ClassifiedMessage;

import java.util.List;

/**
 * Runs message classifiers in a fixed order; the first confident match
 * wins. A message no classifier recognizes is preserved as
 * {@link net.zaminmc.zea.chat.model.MessageCategory#UNCLASSIFIED} —
 * classification failure never hides a message.
 *
 * <p>Order (specific before generic): writing-channel report, private
 * message, death, vote, crate win, Discord, system, global. Discord runs
 * before global so a {@code [Discord]} header is never read as an
 * affiliation bracket; the writing-channel report runs first because it
 * feeds server channel state. Classifiers for the local, trade, nation
 * and town channel formats are pending observed fixtures (see
 * docs/CHAT_CLASSIFICATION.md).
 */
public final class ClassificationPipeline {
	private final List<MessageClassifier> classifiers;

	public ClassificationPipeline() {
		this(List.of(
				new WritingChannelReportClassifier(),
				new PrivateMessageClassifier(),
				new DeathClassifier(),
				new VoteClassifier(),
				new CrateWinClassifier(),
				new DiscordClassifier(),
				new SystemClassifier(),
				new GlobalChatClassifier()));
	}

	ClassificationPipeline(List<MessageClassifier> classifiers) {
		this.classifiers = List.copyOf(classifiers);
	}

	public ClassifiedMessage classify(CapturedMessage message) {
		for (MessageClassifier classifier : classifiers) {
			var result = classifier.tryClassify(message);
			if (result.isPresent()) {
				var found = result.get();
				return new ClassifiedMessage(message, found.category(), found.payload());
			}
		}
		return ClassifiedMessage.unclassified(message);
	}
}
