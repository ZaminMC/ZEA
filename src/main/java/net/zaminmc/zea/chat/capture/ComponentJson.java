package net.zaminmc.zea.chat.capture;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * Serializes a live component tree to its vanilla JSON form — the same
 * shape the server put on the wire — through the shared component codec
 * with plain JSON ops, which needs no registry access.
 *
 * <p>What that means for captured data: this is the wire form, so style
 * insertion (a client-only construct that never crosses the network) is
 * not part of it, and hover events carrying items or entities cannot
 * encode without registries — those surface as errors on the result,
 * never as crashes. Text, colors, flags, hover text and click events,
 * the things EarthMC chat is built from, all round-trip.
 */
public final class ComponentJson {
	private ComponentJson() {
	}

	/** The serialized tree, or the reason it could not be. */
	public record Result(JsonElement json, String error) {
		public boolean ok() {
			return json != null;
		}
	}

	public static Result serialize(Component component) {
		DataResult<JsonElement> encoded;
		try {
			encoded = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component);
		} catch (RuntimeException e) {
			return new Result(null, e.toString());
		}
		return encoded.result()
				.map(json -> new Result(json, null))
				.orElseGet(() -> new Result(null, encoded.error()
						.map(DataResult.Error::message)
						.orElse("unknown serialization failure")));
	}
}
