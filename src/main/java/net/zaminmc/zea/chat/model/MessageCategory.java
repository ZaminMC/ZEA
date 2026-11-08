package net.zaminmc.zea.chat.model;

/**
 * What kind of EarthMC message this is. Categories drive the Stage 3 feed
 * model; they are distinct from feeds (views) and from server channels
 * (writable destinations).
 */
public enum MessageCategory {
	GLOBAL,
	LOCAL,
	TRADE,
	NATION,
	TOWN,
	PRIVATE_MESSAGE,
	DISCORD,
	VOTE,
	CRATE_WIN,
	DEATH,
	SYSTEM,
	UNCLASSIFIED
}
