package net.zaminmc.zea.chat.feeds;

import java.util.List;
import java.util.Optional;

/**
 * The writable EarthMC server channels (verified table; mirrors ZEA-REPO
 * {@code datasets/channels.json}, which becomes the runtime source once
 * the repository client exists).
 */
public final class Channels {
	public static final WriteChannel GLOBAL = new WriteChannel("global", "Global", "g");
	public static final WriteChannel LOCAL = new WriteChannel("local", "Local", "local");
	public static final WriteChannel TRADE = new WriteChannel("trade", "Trade", "tradechat");
	public static final WriteChannel NATION = new WriteChannel("nation", "Nation", "nc");
	public static final WriteChannel TOWN = new WriteChannel("town", "Town", "tc");

	public static List<WriteChannel> all() {
		return List.of(GLOBAL, LOCAL, TRADE, NATION, TOWN);
	}

	public static Optional<WriteChannel> byId(String id) {
		return all().stream().filter(channel -> channel.id().equals(id)).findFirst();
	}

	private Channels() {
	}
}
