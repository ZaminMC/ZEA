package net.zaminmc.zea.notify;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The production {@link NotificationSurface}: the coordinator's writes,
 * against the vanilla HUD. Fetched lazily per call — the client instance
 * does not exist while the mod initializes, only once a message or tick
 * actually arrives.
 */
public final class HudNotificationSurface implements NotificationSurface {
        @Override
        public void showActionBar(Component text) {
                hud().setOverlayMessage(text, false);
        }

        @Override
        public void showTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks,
                        int fadeOutTicks) {
                var hud = hud();
                hud.setTimes(fadeInTicks, stayTicks, fadeOutTicks);
                if (subtitle != null) {
                        hud.setSubtitle(subtitle);
                }
                hud.setTitle(title);
        }

        @Override
        public void clear() {
                // Titles are cleared explicitly; the action bar has no clear in
                // vanilla and simply fades out on its own (~3 s).
                hud().clearTitles();
        }

        private static net.minecraft.client.gui.Hud hud() {
                return Minecraft.getInstance().gui.hud;
        }
}
