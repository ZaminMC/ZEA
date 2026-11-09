package net.zaminmc.zea.share;

/**
 * A failure a user can act on: the paste service being unreachable, a
 * rejected import, an unsupported share version. The message is written
 * for the player, not the log — commands show it verbatim.
 */
public final class ShareException extends Exception {
	public ShareException(String message) {
		super(message);
	}

	public ShareException(String message, Throwable cause) {
		super(message, cause);
	}
}
