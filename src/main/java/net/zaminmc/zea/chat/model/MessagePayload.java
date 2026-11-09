package net.zaminmc.zea.chat.model;

/**
 * Structured payload attached to a classified message, or {@link #NONE}
 * when no structure was confidently recognized.
 *
 * <p>Sealed so consumers (feeds, notifications, history) can pattern-match
 * exhaustively over every known structure.
 */
public sealed interface MessagePayload permits
		MessagePayload.None,
		ChannelChat,
		TaggedChannelChat,
		PrivateMessage,
		DiscordMessage,
		VoteMessage,
		CrateWinMessage,
		DeathMessage,
		SystemMessage,
		WritingChannelReport {

	/** No structured payload; the original component is all there is. */
	record None() implements MessagePayload {
		public static final None INSTANCE = new None();
	}
}
