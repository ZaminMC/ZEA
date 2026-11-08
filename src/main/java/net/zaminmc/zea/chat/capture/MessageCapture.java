package net.zaminmc.zea.chat.capture;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.zaminmc.zea.chat.model.CapturedMessage;

import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Incoming message capture. Hooks Fabric's client receive events — the
 * official integration point, so no custom mixins are needed for capture —
 * and forwards immutable captures to a sink.
 *
 * <p>Action bar text (overlay messages) is not chat and is skipped. Events
 * fire on the game thread, which is where classification runs; it is cheap
 * (anchored matching over short strings) and allocation-light.
 */
public final class MessageCapture {
	public MessageCapture(BooleanSupplier enabled, Consumer<CapturedMessage> sink) {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && enabled.getAsBoolean()) {
				sink.accept(CapturedMessage.of(message));
			}
		});
		ClientReceiveMessageEvents.CHAT.register((message, playerMessage, senderProfile, type, timestamp) -> {
			if (enabled.getAsBoolean()) {
				sink.accept(CapturedMessage.of(message, timestamp == null ? Instant.now() : timestamp));
			}
		});
	}
}
