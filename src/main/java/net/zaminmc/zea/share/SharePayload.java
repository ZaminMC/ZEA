package net.zaminmc.zea.share;

/**
 * A share category's payload: the {@code data} object of a
 * {@link ShareDocument}, able to serialize for export and to describe
 * itself for the import preview. Decoding lives in each type's codec as
 * a static factory; applying an imported payload is an explicit,
 * separate step so nothing can apply half-validated data.
 */
public interface SharePayload {
	/** A one-line, player-readable description of the contents. */
	String summary();
}
