package net.zaminmc.zea.notify;

import net.minecraft.network.chat.Component;

/**
 * The display areas the notification coordinator owns. Production
 * implements this against the HUD; tests record calls instead. No feature
 * may touch the action bar or titles directly — everything routes through
 * {@link NotificationCoordinator}, which is the only writer.
 */
public interface NotificationSurface {
        /** Shows text on the action bar (vanilla keeps it ~3 seconds). */
        void showActionBar(Component text);

        /**
         * Shows text as a title and subtitle. Times are in ticks, matching
         * the vanilla title packets.
         */
        void showTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks);

        /** Clears whatever the coordinator last wrote (both areas). */
        void clear();
}
