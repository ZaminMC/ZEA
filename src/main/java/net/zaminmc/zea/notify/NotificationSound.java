package net.zaminmc.zea.notify;

/**
 * Plays the attention cue for a notification. Injected so tests stay
 * silent; production plays a vanilla UI sound.
 */
@FunctionalInterface
public interface NotificationSound {
	/** A sound that does nothing — for tests and muted setups. */
	NotificationSound NONE = kind -> {
	};

	void play(NotificationKind kind);
}
