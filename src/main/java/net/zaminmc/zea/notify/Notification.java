package net.zaminmc.zea.notify;

import net.minecraft.network.chat.Component;

/**
 * One notification, as declared by its source. Immutable.
 *
 * <p>Two shapes exist, and they carry the coordinator's whole duration
 * policy:
 *
 * <ul>
 *   <li><b>Temporary</b> — shows for {@code durationMillis}, then expires
 *       on its own (mentions).</li>
 *   <li><b>Persistent</b> — shows until its {@code key} is resolved (a PM
 *       replied to) or until {@code expiryMillis} of hard lifetime has
 *       passed (mentions of the surface can never linger forever).
 *       The action bar fades after ~3 seconds in vanilla, so a persistent
 *       notification is periodically re-shown while it stays active.</li>
 * </ul>
 *
 * @param kind           notification kind; carries the priority
 * @param text           what to display
 * @param destination    which surface area to use
 * @param durationMillis temporary lifetime; {@code 0} when persistent
 * @param expiryMillis   hard lifetime for persistent notifications;
 *                       {@code 0} when temporary
 * @param key            resolution key for persistent notifications
 *                       (e.g. the PM sender's name); {@code null} otherwise
 */
public record Notification(NotificationKind kind, Component text, Destination destination,
                long durationMillis, long expiryMillis, String key) {

        /** Where the notification is displayed. */
        public enum Destination {
                ACTION_BAR("Action bar"),
                TITLE("Title");

                private final String label;

                Destination(String label) {
                        this.label = label;
                }

                /** What the MoulConfig dropdown shows for this choice. */
                @Override
                public String toString() {
                        return label;
                }
        }

        public Notification {
                if (kind == null || text == null || destination == null) {
                        throw new NullPointerException("notification fields must not be null");
                }
                if (durationMillis < 0 || expiryMillis < 0) {
                        throw new IllegalArgumentException("notification durations must not be negative");
                }
                if (durationMillis > 0 && expiryMillis > 0) {
                        throw new IllegalArgumentException("a notification is temporary or persistent, not both");
                }
                if (durationMillis == 0 && expiryMillis == 0) {
                        throw new IllegalArgumentException("a notification must have a lifetime");
                }
                if (expiryMillis > 0 && key == null) {
                        throw new IllegalArgumentException("a persistent notification needs a resolution key");
                }
        }

        /** A notification that shows for the given duration and then expires. */
        public static Notification temporary(NotificationKind kind, Component text, Destination destination,
                        long durationMillis) {
                return new Notification(kind, text, destination, durationMillis, 0, null);
        }

        /** A notification that shows until resolved by key or after the hard expiry. */
        public static Notification persistent(NotificationKind kind, Component text, Destination destination,
                        long expiryMillis, String key) {
                return new Notification(kind, text, destination, 0, expiryMillis, key);
        }

        public boolean isPersistent() {
                return expiryMillis > 0;
        }
}
