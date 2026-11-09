package net.zaminmc.zea.share;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.zaminmc.zea.buttons.ButtonAnchor;
import net.zaminmc.zea.buttons.InventoryButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The inventory-button share payload. Export writes every usable button;
 * import is strict — an unusable entry, a duplicated identity or an
 * oversized set rejects the whole import, because an import replaces the
 * player's set wholesale and must never apply half-broken data.
 *
 * <p>The wire shape mirrors the button store's file schema (same fields,
 * same defaults) but is versioned independently through the share
 * envelope — the two formats may evolve for different reasons.
 */
public final class InventoryButtonsPayload implements SharePayload {
	/** Hard cap on a shared set; a real setup is nowhere near this. */
	public static final int MAX_BUTTONS = 100;

	private static final Gson GSON = new Gson();

	private final List<InventoryButton> buttons;

	private InventoryButtonsPayload(List<InventoryButton> buttons) {
		this.buttons = List.copyOf(buttons);
	}

	/** The buttons this payload carries, in order. */
	public List<InventoryButton> buttons() {
		return buttons;
	}

	@Override
	public String summary() {
		if (buttons.isEmpty()) {
			return "No buttons";
		}
		List<String> names = new ArrayList<>();
		for (InventoryButton button : buttons) {
			names.add(button.name());
			if (names.size() == 5) {
				break;
			}
		}
		String shown = String.join(", ", names);
		int more = buttons.size() - names.size();
		return buttons.size() + (buttons.size() == 1 ? " button: " : " buttons: ") + shown
				+ (more > 0 ? " and " + more + " more" : "");
	}

	/**
	 * Builds an export payload. Unusable buttons (an unfinished edit)
	 * are skipped, not exported.
	 *
	 * @param buttons the live button set
	 * @param skipped receives how many unusable buttons were left out
	 */
	public static InventoryButtonsPayload of(List<InventoryButton> buttons, int[] skipped) {
		List<InventoryButton> usable = new ArrayList<>();
		int unusable = 0;
		for (InventoryButton button : buttons == null ? List.<InventoryButton>of() : buttons) {
			if (button.isUsable()) {
				usable.add(button);
			} else {
				unusable++;
			}
		}
		if (skipped != null && skipped.length == 1) {
			skipped[0] = unusable;
		}
		return new InventoryButtonsPayload(usable);
	}

	/**
	 * Decodes and validates an imported payload.
	 *
	 * @throws ShareException when any entry is unusable, an identity
	 *                         repeats, or the set exceeds {@link #MAX_BUTTONS}
	 */
	public static InventoryButtonsPayload fromJson(JsonObject data) throws ShareException {
		List<ButtonWire> entries = readButtons(data);
		List<InventoryButton> parsed = new ArrayList<>();
		Set<UUID> ids = new HashSet<>();
		int index = 0;
		for (ButtonWire entry : entries) {
			index++;
			if (entry == null) {
				throw new ShareException("Button " + index + " is damaged in that share");
			}
			InventoryButton button = entry.toButton();
			if (!button.isUsable()) {
				throw new ShareException("Button '" + button.name() + "' has no command or "
						+ "icon set — the share looks unfinished");
			}
			if (!ids.add(button.id())) {
				throw new ShareException("Two buttons share one identity in that share");
			}
			parsed.add(button);
		}
		if (parsed.size() > MAX_BUTTONS) {
			throw new ShareException("That share has " + parsed.size() + " buttons; ZEA imports "
					+ "at most " + MAX_BUTTONS);
		}
		return new InventoryButtonsPayload(parsed);
	}

	/** Serializes the payload's {@code data} object. */
	public JsonObject toJson() {
		JsonArray array = new JsonArray();
		for (InventoryButton button : buttons) {
			array.add(GSON.toJsonTree(ButtonWire.from(button)));
		}
		JsonObject data = new JsonObject();
		data.add("buttons", array);
		return data;
	}

	private static List<ButtonWire> readButtons(JsonObject data) throws ShareException {
		try {
			JsonElement element = data.get("buttons");
			if (element == null || element.isJsonNull()) {
				return List.of();
			}
			if (!element.isJsonArray()) {
				throw new ShareException("The buttons in that share are not a list");
			}
			List<ButtonWire> entries = new ArrayList<>();
			for (JsonElement item : element.getAsJsonArray()) {
				entries.add(item.isJsonNull() ? null : GSON.fromJson(item, ButtonWire.class));
			}
			return entries;
		} catch (JsonParseException | IllegalStateException e) {
			throw new ShareException("The buttons in that share are damaged", e);
		}
	}

	/** One button as it appears in the share; every field optional, like the store's file. */
	private record ButtonWire(
			UUID id,
			String name,
			String command,
			String icon,
			ButtonAnchor anchor,
			Integer offsetX,
			Integer offsetY,
			Boolean enabled) {
		InventoryButton toButton() {
			return new InventoryButton(
					id,
					name,
					command,
					icon,
					anchor,
					offsetX == null ? InventoryButton.DEFAULT_OFFSET_X : offsetX,
					offsetY == null ? InventoryButton.DEFAULT_OFFSET_Y : offsetY,
					enabled == null || enabled);
		}

		static ButtonWire from(InventoryButton button) {
			return new ButtonWire(
					button.id(),
					button.name(),
					button.command(),
					button.icon(),
					button.anchor(),
					button.offsetX(),
					button.offsetY(),
					button.enabled());
		}
	}
}
