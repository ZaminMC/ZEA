package net.zaminmc.zea.chat.model;

import net.minecraft.network.chat.Component;

import java.time.Instant;

/**
 * An incoming chat message, captured verbatim.
 *
 * <p>Immutable. The original component (formatting, colors, hover and click
 * events, insertion) is always retained — every later representation of the
 * message is additive, never a replacement.
 *
 * @param original   the message exactly as received
 * @param text       flattened text of {@code original}, computed once at capture
 * @param receivedAt capture timestamp
 */
public record CapturedMessage(Component original, String text, Instant receivedAt) {
	public CapturedMessage {
		if (original == null || text == null || receivedAt == null) {
			throw new NullPointerException("captured message fields must not be null");
		}
	}

	public static CapturedMessage of(Component original) {
		return of(original, Instant.now());
	}

	public static CapturedMessage of(Component original, Instant receivedAt) {
		return new CapturedMessage(original, original.getString(), receivedAt);
	}
}
