package net.zaminmc.zea.chat.model;

import java.util.Optional;

/**
 * A player chat message on a tagged server channel (observed: local,
 * trade, nation, town). The leading bracket names the channel; nation
 * chat additionally carries the sender's town inside the bracket
 * (observed: {@code [Nation Chat|Sosnovka]}). Structured segments are
 * parsed while the original component — colors, formatting, hover and
 * click events — stays attached to the enclosing {@link ClassifiedMessage}.
 *
 * @param tag         the bracket label, e.g. {@code "Local"} or {@code "Nation Chat"}
 * @param town        the sender's town, present on nation chat only
 * @param displayName name as displayed (may be a nickname)
 * @param content     the message itself
 */
public record TaggedChannelChat(String tag, Optional<String> town, String displayName, String content)
		implements MessagePayload {
	public TaggedChannelChat {
		if (tag == null || town == null || displayName == null || content == null) {
			throw new NullPointerException("tagged channel chat fields must not be null");
		}
		if (tag.isBlank()) {
			throw new IllegalArgumentException("tag must not be blank");
		}
		if (displayName.isBlank()) {
			throw new IllegalArgumentException("display name must not be blank");
		}
	}
}
