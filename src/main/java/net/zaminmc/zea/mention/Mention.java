package net.zaminmc.zea.mention;

import net.zaminmc.zea.chat.model.CapturedMessage;

/**
 * A detected mention of the local player.
 *
 * @param source  the message the mention appeared in; its original
 *                component is what the chat highlight keys on
 * @param speaker who said it — the channel display name, or the word
 *                before the separator for unstructured messages
 * @param content the attributed text the mention appeared in
 */
public record Mention(CapturedMessage source, String speaker, String content) {
        public Mention {
                if (source == null || speaker == null || content == null) {
                        throw new NullPointerException("mention fields must not be null");
                }
                if (speaker.isBlank()) {
                        throw new IllegalArgumentException("speaker must not be blank");
                }
        }
}
