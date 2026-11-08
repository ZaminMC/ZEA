package net.zaminmc.zea.mention;

/**
 * Receives detected mentions for the (opt-in) auto-response path. Split
 * from {@link Mentions} so the responder's guards stay independently
 * testable and the none-implementation is trivial.
 */
public interface MentionResponder {
        /** A mention-eligible no-op; the default until responses are enabled. */
        MentionResponder NONE = (mention, now) -> {
        };

        void onMention(Mention mention, long now);
}
