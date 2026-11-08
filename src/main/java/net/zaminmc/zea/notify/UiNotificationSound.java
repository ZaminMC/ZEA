package net.zaminmc.zea.notify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The production {@link NotificationSound}: a soft experience-orb pickup,
 * pitched down for private messages so the two cues are distinguishable.
 * Uses the UI path (no world attenuation), like vanilla's own menu sounds.
 */
public final class UiNotificationSound implements NotificationSound {
        private static final float MENTION_PITCH = 1.0F;
        private static final float PRIVATE_MESSAGE_PITCH = 0.6F;

        @Override
        public void play(NotificationKind kind) {
                float pitch = switch (kind) {
                        case PRIVATE_MESSAGE -> PRIVATE_MESSAGE_PITCH;
                        case MENTION -> MENTION_PITCH;
                };
                Minecraft.getInstance().getSoundManager()
                                .play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch));
        }
}
