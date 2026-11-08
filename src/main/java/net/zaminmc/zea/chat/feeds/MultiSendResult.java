package net.zaminmc.zea.chat.feeds;

/** Outcome of a multi-target send request, surfaced to the chat UI. */
public enum MultiSendResult {
	/** The batch was accepted and is dispatching with the configured interval. */
	DISPATCHING,

	/** Multi-channel sending is disabled in configuration. */
	DISABLED,

	/** No channels were selected. */
	NO_TARGETS
}
