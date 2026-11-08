package net.zaminmc.zea.chat.feeds;

import net.zaminmc.zea.chat.model.MessageCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The feed definitions. Default-visible tabs follow the Phase 1 spec:
 * All, Global, Local, Trade, Nation, Town, PMs; the remaining feeds are
 * accessible but hidden until enabled in configuration (Stage 3+ wiring).
 *
 * <p>Feeds are data. When ZEA-REPO consumption lands, channel-derived
 * feeds are built from the channels dataset; category feeds (events,
 * system, unclassified) remain code-level.
 */
public final class Feeds {
        /** Aggregates every category, including unclassified. */
        public static final Feed ALL = new Feed("all", "All", Set.of(MessageCategory.values()));

        public static final Feed GLOBAL = channelFeed(MessageCategory.GLOBAL);
        public static final Feed LOCAL = channelFeed(MessageCategory.LOCAL);
        public static final Feed TRADE = channelFeed(MessageCategory.TRADE);
        public static final Feed NATION = channelFeed(MessageCategory.NATION);
        public static final Feed TOWN = channelFeed(MessageCategory.TOWN);
        public static final Feed PRIVATE_MESSAGES = new Feed("pms", "PMs",
                        Set.of(MessageCategory.PRIVATE_MESSAGE));
        public static final Feed DISCORD = new Feed("discord", "Discord", Set.of(MessageCategory.DISCORD));
        public static final Feed VOTES = new Feed("votes", "Votes", Set.of(MessageCategory.VOTE));
        public static final Feed CRATE_WINS = new Feed("crates", "Crate Wins", Set.of(MessageCategory.CRATE_WIN));
        public static final Feed DEATHS = new Feed("deaths", "Deaths", Set.of(MessageCategory.DEATH));
        public static final Feed SYSTEM = new Feed("system", "System", Set.of(MessageCategory.SYSTEM));
        public static final Feed UNCLASSIFIED = new Feed("unclassified", "General",
                        Set.of(MessageCategory.UNCLASSIFIED));

        private static Feed channelFeed(MessageCategory category) {
                return new Feed(category.name().toLowerCase(Locale.ROOT),
                                category.name().charAt(0) + category.name().substring(1).toLowerCase(Locale.ROOT),
                                Set.of(category));
        }

        public static List<Feed> allFeeds() {
                return List.of(ALL, GLOBAL, LOCAL, TRADE, NATION, TOWN, PRIVATE_MESSAGES,
                                DISCORD, VOTES, CRATE_WINS, DEATHS, SYSTEM, UNCLASSIFIED);
        }

        /** Feeds shown as tabs before the user configures anything. */
        public static List<Feed> defaultVisibleTabs() {
                return List.of(ALL, GLOBAL, LOCAL, TRADE, NATION, TOWN, PRIVATE_MESSAGES);
        }

        public static List<Feed> extraFeeds() {
                List<Feed> extras = new ArrayList<>(allFeeds());
                extras.removeAll(defaultVisibleTabs());
                return List.copyOf(extras);
        }

        private Feeds() {
        }
}
