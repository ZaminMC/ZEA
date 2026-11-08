package net.zaminmc.zea.chat.classify;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;
import net.zaminmc.zea.chat.model.MessagePayload;

import java.util.Optional;

/**
 * One message family recognizer.
 *
 * <p>Confidence is binary by design: a classifier either recognizes the
 * message with an anchored structural pattern and returns a result, or
 * declines ({@link Optional#empty()}). There is no fuzzy partial match —
 * every pattern is anchored to the full observed shape, so a match is
 * always confident. A message no classifier recognizes is preserved as
 * {@link MessageCategory#UNCLASSIFIED} by the pipeline.
 */
public interface MessageClassifier {
	Optional<Result> tryClassify(CapturedMessage message);

	/**
	 * @param category recognized category
	 * @param payload  structured data, or {@link MessagePayload.None} when the
	 *                 family carries no structure beyond its category
	 */
	record Result(MessageCategory category, MessagePayload payload) {
		public Result {
			if (category == null || payload == null) {
				throw new NullPointerException("result fields must not be null");
			}
		}

		public static Result of(MessageCategory category, MessagePayload payload) {
			return new Result(category, payload);
		}

		public static Result of(MessageCategory category) {
			return new Result(category, MessagePayload.None.INSTANCE);
		}
	}
}
