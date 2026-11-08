package net.zaminmc.zea.chat.feeds;

import java.util.List;

/**
 * A writable EarthMC server channel, with the command used to send a
 * single message to it (verified syntax; the table mirrors ZEA-REPO
 * {@code datasets/channels.json}).
 *
 * <p>Only the per-message command form is modeled. Standalone switch
 * commands (e.g. {@code /global} changing the server's writing channel)
 * are deliberately not represented as an action ZEA performs: the server
 * writing channel changes only through the player's own commands, and
 * ZEA tracks it from the server's reports instead.
 *
 * @param id             stable identifier
 * @param label          display label
 * @param messageCommand command that sends one message (without slash)
 */
public record WriteChannel(String id, String label, String messageCommand) {
	public WriteChannel {
		if (id == null || label == null || messageCommand == null
				|| id.isBlank() || label.isBlank() || messageCommand.isBlank()) {
			throw new IllegalArgumentException("write channel fields must be non-blank");
		}
	}
}
