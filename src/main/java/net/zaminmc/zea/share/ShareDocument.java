package net.zaminmc.zea.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The versioned share envelope:
 *
 * <pre>{@code
 * { "format": "zea-share", "type": "inventory-buttons", "version": 1, "data": { } }
 * }</pre>
 *
 * <p>Parsing is strict about identity and version — the format marker must
 * match exactly, the type must be known, and a version this build does not
 * understand is rejected rather than guessed. Envelope fields beyond the
 * four above are ignored, so a newer producer can add metadata without
 * breaking older consumers. Payload validation is not this class's job;
 * each {@link ShareType} owns its codec.
 */
public record ShareDocument(ShareType type, int version, JsonObject data) {
	/** The envelope marker; anything else is not a ZEA share. */
	public static final String FORMAT = "zea-share";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public ShareDocument {
		if (type == null) {
			throw new NullPointerException("type must not be null");
		}
		data = data == null ? new JsonObject() : data;
	}

	/** Creates a document at the type's current schema version. */
	public static ShareDocument of(ShareType type, JsonObject data) {
		return new ShareDocument(type, type.currentVersion(), data);
	}

	/**
	 * Parses and checks the envelope.
	 *
	 * @throws ShareException with a player-readable reason for every
	 *                         rejected shape
	 */
	public static ShareDocument parse(String json) throws ShareException {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new ShareException("That paste is not valid JSON", e);
		}
		if (!root.isJsonObject()) {
			throw new ShareException("That paste is not a JSON object");
		}
		JsonObject object = root.getAsJsonObject();

		JsonElement format = object.get("format");
		if (format == null || !format.isJsonPrimitive() || !FORMAT.equals(format.getAsString())) {
			throw new ShareException("That paste is not a ZEA share document");
		}

		JsonElement typeElement = object.get("type");
		if (typeElement == null || !typeElement.isJsonPrimitive()) {
			throw new ShareException("The share document names no type");
		}
		ShareType type;
		try {
			type = ShareType.from(typeElement.getAsString());
		} catch (ShareException e) {
			throw new ShareException("The share document has an unknown type '"
					+ typeElement.getAsString() + "'");
		}

		JsonElement version = object.get("version");
		if (version == null || !version.isJsonPrimitive() || !isWholeNumber(version)) {
			throw new ShareException("The share document names no version");
		}
		int parsed = version.getAsInt();
		if (parsed < 1 || parsed > type.currentVersion()) {
			throw new ShareException("The share uses version " + parsed + " of '"
					+ type.id() + "', but this ZEA understands up to version "
					+ type.currentVersion() + " — update ZEA to import it");
		}

		JsonElement data = object.get("data");
		if (data != null && !data.isJsonObject()) {
			throw new ShareException("The share document's data is not a JSON object");
		}
		return new ShareDocument(type, parsed, data == null ? new JsonObject() : data.getAsJsonObject());
	}

	/** Serializes for upload; pretty-printed so pastes stay human-inspectable. */
	public String toJson() {
		JsonObject envelope = new JsonObject();
		envelope.addProperty("format", FORMAT);
		envelope.addProperty("type", type.id());
		envelope.addProperty("version", version);
		envelope.add("data", data);
		return GSON.toJson(envelope);
	}

	private static boolean isWholeNumber(JsonElement element) {
		try {
			return element.getAsBigDecimal().scale() <= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
