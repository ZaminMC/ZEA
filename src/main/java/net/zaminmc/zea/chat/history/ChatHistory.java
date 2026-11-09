package net.zaminmc.zea.chat.history;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * The in-memory chat history for the current session: a bounded,
 * chronological buffer of recorded entries.
 *
 * <p>This is the single in-memory representation regardless of whether
 * persistence is enabled — with persistence off it is exactly the
 * session history (cleared on disconnect), with persistence on it is
 * seeded from the per-server file on join and flushed back
 * periodically. The capacity comes from the configured history size, so
 * memory is bounded by the same knob that bounds the file.
 *
 * <p>Thread confinement: appends, merges and clears happen on the
 * client thread; snapshots (for search and serialization) may be taken
 * from background executors. All access is therefore guarded by one
 * lock, and every reader gets an immutable copy — never a live view.
 */
public final class ChatHistory {
        private final int capacity;
        private final ArrayDeque<HistoryEntry> entries = new ArrayDeque<>();
        private final Object lock = new Object();

        /**
         * @param capacity the maximum number of retained entries; values
         *                 below 1 are treated as 1 (a history that forgets
         *                 everything is never useful)
         */
        public ChatHistory(int capacity) {
                this.capacity = Math.max(1, capacity);
        }

        /** Appends one entry, evicting the oldest beyond capacity. */
        public void append(HistoryEntry entry) {
                if (entry == null) {
                        throw new NullPointerException("history entry must not be null");
                }
                synchronized (lock) {
                        entries.addLast(entry);
                        while (entries.size() > capacity) {
                                entries.removeFirst();
                        }
                }
        }

        /**
         * Merges previously persisted entries in front of the session's own —
         * they are chronologically older — keeping both lists' order, then
         * re-enforces capacity.
         */
        public void prependAll(List<HistoryEntry> older) {
                if (older == null || older.isEmpty()) {
                        return;
                }
                synchronized (lock) {
                        List<HistoryEntry> session = new ArrayList<>(entries);
                        entries.clear();
                        entries.addAll(older);
                        entries.addAll(session);
                        trimToCapacity();
                }
        }

        /**
         * Enforces retention: entries older than {@code cutoff} are dropped
         * wherever they sit, and at most {@code maxEntries} newest remain.
         *
         * @return how many entries were removed
         */
        public int trim(Instant cutoff, int maxEntries) {
                int limit = Math.max(0, maxEntries);
                int removed = 0;
                synchronized (lock) {
                        List<HistoryEntry> kept = new ArrayList<>(entries.size());
                        for (HistoryEntry entry : entries) {
                                if (entry.receivedAt().isBefore(cutoff)) {
                                        removed++;
                                } else {
                                        kept.add(entry);
                                }
                        }
                        if (kept.size() > limit) {
                                removed += kept.size() - limit;
                                kept = kept.subList(kept.size() - limit, kept.size());
                        }
                        if (removed > 0) {
                                entries.clear();
                                entries.addAll(kept);
                        }
                        return removed;
                }
        }

        /** An immutable copy of the current entries, oldest first. */
        public List<HistoryEntry> snapshot() {
                synchronized (lock) {
                        return List.copyOf(entries);
                }
        }

        /** The number of retained entries. */
        public int size() {
                synchronized (lock) {
                        return entries.size();
                }
        }

        /** Drops everything; used when the session ends. */
        public void clear() {
                synchronized (lock) {
                        entries.clear();
                }
        }

        private void trimToCapacity() {
                while (entries.size() > capacity) {
                        entries.removeFirst();
                }
        }
}
