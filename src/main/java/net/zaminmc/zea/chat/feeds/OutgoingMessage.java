package net.zaminmc.zea.chat.feeds;

/**
 * One outgoing chat command, ready to send.
 *
 * @param command   command name without the leading slash ("g", "local")
 * @param arguments the message text
 */
public record OutgoingMessage(String command, String arguments) {
	public OutgoingMessage {
		if (command == null || arguments == null || command.isBlank()) {
			throw new IllegalArgumentException("command must be non-blank and arguments non-null");
		}
	}

	/** The full command line as typed, without the leading slash. */
	public String commandLine() {
		return command + " " + arguments;
	}
}
