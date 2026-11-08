package net.zaminmc.zea.notify;

/**
 * What kind of notification this is. The set is deliberately tiny — the
 * coordinator's whole policy is expressed through it (see
 * ARCHITECTURE.md §11); extending it requires updating that document.
 *
 * <p>Priority: a private message outranks a mention, so a PM arriving
 * while a mention shows replaces it, never the other way around.
 */
public enum NotificationKind {
        PRIVATE_MESSAGE(2),
        MENTION(1);

        private final int priority;

        NotificationKind(int priority) {
                this.priority = priority;
        }

        /** Higher wins the surface; equal priority queues. */
        public int priority() {
                return priority;
        }
}
