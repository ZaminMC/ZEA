package net.zaminmc.zea.chat.model;

import java.util.List;

/**
 * A player chat message on a writable server channel (observed format:
 * global). Structured segments are parsed while the original component —
 * including all formatting and interactivity — stays attached to the
 * enclosing {@link ClassifiedMessage}.
 *
 * @param badges      badge symbols preceding the affiliation, in order
 * @param affiliation nation/town segment
 * @param displayName name as displayed (may be a nickname)
 * @param content     the message itself
 */
public record ChannelChat(List<Badge> badges, Affiliation affiliation, String displayName, String content)
		implements MessagePayload {
	public ChannelChat {
		if (badges == null || affiliation == null || displayName == null || content == null) {
			throw new NullPointerException("channel chat fields must not be null");
		}
		badges = List.copyOf(badges);
		if (displayName.isBlank()) {
			throw new IllegalArgumentException("display name must not be blank");
		}
	}
}
