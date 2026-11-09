package net.zaminmc.zea.chat.history;

import net.zaminmc.zea.config.ZeaConfig;
import net.zaminmc.zea.earthmc.ServerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Coordinates persistent chat history: seeding the in-memory history
 * from the per-server file on join, batching writes onto a background
 * executor, and enforcing retention on a schedule — never per write.
 *
 * <p>Threading contract: {@link #start}, {@link #markDirty}, {@link #tick},
 * {@link #flush} and {@link #stop} are confined to the client thread;
 * the executor runs the file IO, and its results return through
 * {@code toClientThread}. Writes coalesce through a {@code writing}
 * flag — while one save is in flight no second is submitted, and the
 * dirt recorded meanwhile goes out with the next flush. Because the
 * executor is sequential and the load is submitted before any save can
 * be, the first save of a session always observes the file the load
 * read, so an in-flight load can never be clobbered.
 *
 * <p>Configuration is sampled per session: toggling persistence or
 * changing retention takes effect on the next join, not retroactively
 * on a running session. History itself is only ever chat text,
 * timestamps and categories — no credentials exist to persist.
 */
public final class HistoryRecorder {
        private static final Logger LOGGER = LoggerFactory.getLogger(HistoryRecorder.class);

        /** Minimum spacing between automatic history writes. */
        public static final long FLUSH_INTERVAL_MILLIS = 30_000;

        /** How often retention (age + size limits) is re-enforced. */
        public static final long RETENTION_INTERVAL_MILLIS = 300_000;

        private final ChatHistory history;
        private final ZeaConfig config;
        private final Executor executor;
        private final LongSupplier nowMillis;
        private final Consumer<Runnable> toClientThread;

        private HistoryStore store;
        private boolean dirty;
        private boolean writing;
        private long lastFlushAt;
        private long lastRetentionAt;

        /**
         * @param history        the session's history buffer
         * @param config         configuration, sampled at {@link #start}
         * @param executor       sequential executor for file IO
         * @param nowMillis      wall clock, injectable for tests
         * @param toClientThread marshals executor results back to the client thread
         */
        public HistoryRecorder(ChatHistory history, ZeaConfig config, Executor executor,
                        LongSupplier nowMillis, Consumer<Runnable> toClientThread) {
                this.history = history;
                this.config = config;
                this.executor = executor;
                this.nowMillis = nowMillis;
                this.toClientThread = toClientThread;
                this.lastRetentionAt = nowMillis.getAsLong();
        }

        /**
         * Begins a session on one server: clears the previous session's
         * entries and, when persistence is enabled, seeds the history from
         * the server's file (asynchronously — never blocking the client
         * thread on IO).
         */
        public void start(Path historyDirectory, ServerIdentity server) {
                history.clear();
                store = null;
                dirty = false;
                if (!config.historyEnabled()) {
                        return;
                }
                HistoryStore fresh = new HistoryStore(historyDirectory.resolve(server.fileSafeName() + ".json"));
                store = fresh;
                if (!submit(() -> {
                        List<HistoryEntry> loaded = fresh.load(config.historyMaxEntries());
                        toClientThread.accept(() -> {
                                if (store == fresh) {
                                        history.prependAll(loaded);
                                }
                        });
                })) {
                        LOGGER.warn("Could not load chat history for {}; continuing with the session only", server.host());
                }
        }

        /** Marks the history as changed since the last write. */
        public void markDirty() {
                dirty = true;
        }

        /**
         * Periodic maintenance from the client tick: re-enforces retention
         * every {@link #RETENTION_INTERVAL_MILLIS}, then flushes a dirty
         * history at most every {@link #FLUSH_INTERVAL_MILLIS} — retention
         * runs first so a cleanup is persisted by the same tick's write, not
         * the next one.
         */
        public void tick() {
                long now = nowMillis.getAsLong();
                if (store == null) {
                        return;
                }
                if (now - lastRetentionAt >= RETENTION_INTERVAL_MILLIS) {
                        lastRetentionAt = now;
                        Instant cutoff = Instant.ofEpochMilli(now - config.historyRetentionMillis());
                        int removed = history.trim(cutoff, config.historyMaxEntries());
                        if (removed > 0) {
                                dirty = true;
                        }
                }
                if (dirty && !writing && now - lastFlushAt >= FLUSH_INTERVAL_MILLIS) {
                        flush();
                }
        }

        /**
         * Writes the history out if anything changed and no write is in
         * flight; a clean history writes nothing at all.
         */
        public void flush() {
                HistoryStore target = store;
                if (target == null || !dirty || writing) {
                        return;
                }
                List<HistoryEntry> snapshot = history.snapshot();
                writing = true;
                dirty = false;
                lastFlushAt = nowMillis.getAsLong();
                if (!submit(() -> {
                        try {
                                target.save(snapshot);
                        } finally {
                                toClientThread.accept(() -> writing = false);
                        }
                })) {
                        writing = false;
                        dirty = true; // retry on the next tick
                }
        }

        /**
         * Ends the session: flushes pending changes (the executor outlives
         * the connection) and clears the in-memory history. The next join
         * reloads from the file.
         */
        public void stop() {
                flush();
                history.clear();
                store = null;
        }

        /** Whether a write is currently in flight; exposed for tests. */
        boolean writing() {
                return writing;
        }

        private boolean submit(Runnable task) {
                try {
                        executor.execute(task);
                        return true;
                } catch (RejectedExecutionException e) {
                        LOGGER.warn("History executor rejected work (busy or closing): {}", e.toString());
                        return false;
                }
        }
}
