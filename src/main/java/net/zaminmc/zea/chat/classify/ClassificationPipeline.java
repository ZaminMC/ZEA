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
 * message, death, vote, crate win, Discord, system, tagged channel
 * (local/trade/nation/town), global. Discord and the tagged channels run
 * before global so their bracketed headers are never read as affiliation
 * brackets; the writing-channel report runs first because it feeds server
 * channel state. Global is last: its bracket + name + colon anchor is the
 * weakest of the chat shapes.
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
				new TaggedChannelChatClassifier(),
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
