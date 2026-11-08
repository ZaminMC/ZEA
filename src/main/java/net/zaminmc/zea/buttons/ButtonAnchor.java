package net.zaminmc.zea.buttons;

/**
 * The container corner a button is anchored to. Anchors keep buttons near
 * the GUI they belong to across the different container sizes EarthMC
 * uses (inventory, chests, crafting tables), instead of pinning them to
 * screen coordinates.
 *
 * <p>All anchor math lives in {@link ButtonGeometry}; this enum is only
 * the semantic identity of a corner.
 */
public enum ButtonAnchor {
	TOP_LEFT,
	TOP_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_RIGHT
}
