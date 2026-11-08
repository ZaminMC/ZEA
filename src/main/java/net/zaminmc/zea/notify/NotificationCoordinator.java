package net.zaminmc.zea.notify;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

/**
 * The single owner of the action bar, title and subtitle. Every
 * notification — PM notices, mentions — routes through here; no feature
 * writes to those surfaces directly, so nothing can fight over them.
 *
 * <p>Policy (ARCHITECTURE.md §11), implemented as small as it can be:
 *
 * <ul>
 *   <li>Sources declare priority through {@link NotificationKind};
 *       a private message outranks a mention.</li>
 *   <li>A higher-priority notification replaces the one showing; equal
 *       priority queues behind it (bounded — the oldest queued entry is
 *       dropped first when the queue is full).</li>
 *   <li>Persistent notifications show until resolved by key (a PM replied
 *       to) or until their hard expiry; the action bar fades in vanilla
 *       after ~3 seconds, so an active persistent notification is re-shown
 *       periodically to stay visible.</li>
 *   <li>{@link #clear()} empties everything — the disconnect path.</li>
 * </ul>
 *
 * <p>Time is injected ({@code now} in epoch milliseconds) and the display
 * is a {@link NotificationSurface}, so the whole policy is testable
 * without a client. All access happens on the client thread (chat events
 * and the client tick share it).
 */
public final class NotificationCoordinator {
        /** Bounded queue: a busy chat cannot bank endless pending pings. */
        private static final int QUEUE_CAPACITY = 4;

        /**
         * Vanilla keeps an action bar message for 60 ticks (~3 s); re-show
         * a persistent notification slightly inside that window so it
         * never blinks out.
         */
        private static final long REFRESH_MILLIS = 2500;

        /** Title fade in/out, in ticks; stay time derives from the duration. */
        private static final int TITLE_FADE_IN_TICKS = 5;
        private static final int TITLE_FADE_OUT_TICKS = 10;

        private final NotificationSurface surface;
        private final ArrayDeque<Notification> queue = new ArrayDeque<>();
        private Notification showing;
        private long shownAt;
        private long lastRefreshAt;

        public NotificationCoordinator(NotificationSurface surface) {
                if (surface == null) {
                        throw new NullPointerException("surface must not be null");
                }
                this.surface = surface;
        }

        /** Posts a notification, applying the priority policy. */
        public void post(Notification notification, long now) {
                if (notification == null) {
                        return;
                }
                if (showing != null && notification.isPersistent()
                                && notification.key().equals(showing.key())) {
                        // Same source again (e.g. a second PM from the same
                        // sender): update in place, keeping the surface.
                        showing = notification;
                        display(notification, now);
                        return;
                }
                if (showing == null) {
                        display(notification, now);
                        return;
                }
                if (notification.kind().priority() > showing.kind().priority()) {
                        // Preempts; the lower-priority notice is dropped.
                        display(notification, now);
                        return;
                }
                enqueue(notification);
        }

        /** Advances expiry, refresh and queue promotion. */
        public void tick(long now) {
                if (showing == null) {
                        Notification next = queue.poll();
                        if (next != null) {
                                display(next, now);
                        }
                        return;
                }
                long elapsed = now - shownAt;
                boolean expired = showing.isPersistent()
                                ? elapsed >= showing.expiryMillis()
                                : elapsed >= showing.durationMillis();
                if (expired) {
                        dismiss();
                        Notification next = queue.poll();
                        if (next != null) {
                                display(next, now);
                        }
                        return;
                }
                if (showing.isPersistent()
                                && showing.destination() == Notification.Destination.ACTION_BAR
                                && now - lastRefreshAt >= REFRESH_MILLIS) {
                        surface.showActionBar(showing.text());
                        lastRefreshAt = now;
                }
        }

        /**
         * Resolves the persistent notification with this key — the
         * "replied to" path. Clears it whether it is showing or queued.
         *
         * @return whether a notification was resolved
         */
        public boolean resolve(String key, long now) {
                if (key == null || key.isBlank()) {
                        return false;
                }
                boolean resolved = false;
                if (showing != null && showing.isPersistent() && key.equals(showing.key())) {
                        dismiss();
                        Notification next = queue.poll();
                        if (next != null) {
                                display(next, now);
                        }
                        resolved = true;
                }
                resolved |= queue.removeIf(queued -> queued.isPersistent() && key.equals(queued.key()));
                return resolved;
        }

        /** Clears everything; the disconnect path. */
        public void clear() {
                queue.clear();
                if (showing != null) {
                        dismiss();
                }
        }

        /** The notification currently on the surface, if any. */
        public Optional<Notification> showing() {
                return Optional.ofNullable(showing);
        }

        /** How many notifications are waiting behind the showing one. */
        public int queued() {
                return queue.size();
        }

        /** The waiting notifications, oldest first. */
        public List<Notification> queuedNotifications() {
                return List.copyOf(queue);
        }

        private void enqueue(Notification notification) {
                if (queue.size() >= QUEUE_CAPACITY) {
                        queue.pollFirst();
                }
                queue.addLast(notification);
        }

        private void display(Notification notification, long now) {
                showing = notification;
                shownAt = now;
                lastRefreshAt = now;
                if (notification.destination() == Notification.Destination.ACTION_BAR) {
                        surface.showActionBar(notification.text());
                } else {
                        long duration = notification.isPersistent()
                                        ? notification.expiryMillis()
                                        : notification.durationMillis();
                        int stayTicks = Math.max(10, (int) (duration / 50));
                        surface.showTitle(notification.text(), null,
                                        TITLE_FADE_IN_TICKS, stayTicks, TITLE_FADE_OUT_TICKS);
                }
        }

        private void dismiss() {
                showing = null;
                surface.clear();
        }
}
