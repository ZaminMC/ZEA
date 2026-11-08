package net.zaminmc.zea.mention;

import net.zaminmc.zea.chat.model.CapturedMessage;
import net.zaminmc.zea.chat.model.MessageCategory;

/**
 * A detected mention of the local player.
 *
 * @param source   the message the mention appeared in; its original
 *                 component is what the chat highlight keys on
 * @param speaker  who said it — the channel display name, or the word
 *                 before the separator for unstructured messages
 * @param content  the attributed text the mention appeared in
 * @param category the message's classification; also the channel an
 *                 auto-response would be sent to
 */
public record Mention(CapturedMessage source, String speaker, String content, MessageCategory category) {
        public Mention {
                if (source == null || speaker == null || content == null || category == null) {
                        throw new NullPointerException("mention fields must not be null");
                }
                if (speaker.isBlank()) {
                        throw new IllegalArgumentException("speaker must not be blank");
                }
        }
}
