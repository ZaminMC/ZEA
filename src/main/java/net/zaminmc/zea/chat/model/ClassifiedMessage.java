package net.zaminmc.zea.chat.model;

/**
 * A captured message plus its classification result.
 *
 * <p>Immutable. Unclassified messages keep {@link MessageCategory#UNCLASSIFIED}
 * with {@link MessagePayload.None} — classification failure never hides a
 * message.
 *
 * @param captured the original capture, always retained
 * @param category classification
 * @param payload  structured data, or {@link MessagePayload.None}
 */
public record ClassifiedMessage(CapturedMessage captured, MessageCategory category, MessagePayload payload) {
	public ClassifiedMessage {
		if (captured == null || category == null || payload == null) {
			throw new NullPointerException("classified message fields must not be null");
		}
	}

	public static ClassifiedMessage unclassified(CapturedMessage captured) {
		return new ClassifiedMessage(captured, MessageCategory.UNCLASSIFIED, MessagePayload.None.INSTANCE);
	}

	public boolean isUnclassified() {
		return category == MessageCategory.UNCLASSIFIED;
	}
}
