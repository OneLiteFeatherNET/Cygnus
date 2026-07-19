package net.onelitefeather.cygnus.stamina;

import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.SlenderVisibilityChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Utility class to have one single place for the code to trigger the activation of the {@link SlenderBar}.
 * The class is used to trigger the activation of the {@link SlenderBar} and to update the rune status.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("java:S3252")
public final class SlenderBarTrigger {

    private static final long COOLDOWN_TIME = 1_000;
    private static final Sound ABORT_SOUND = Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.MASTER, 1F, 0F);

    private final Supplier<@Nullable StaminaBar> slenderBarSupplier;

    private long lastSoundTimeStamp = 0;

    /**
     * Creates a new instance of this class.
     *
     * @param slenderBarSupplier the supplier to get the {@link SlenderBar}
     */
    public SlenderBarTrigger(Supplier<@Nullable StaminaBar> slenderBarSupplier) {
        this.slenderBarSupplier = slenderBarSupplier;
    }

    /**
     * Triggers the activation of the {@link SlenderBar} for the given {@link Player}.
     *
     * @param player the player to trigger the activation
     */
    public void trigger(Player player) {
        if (!TeamHelper.isSlenderTeam(player)) return;
        SlenderBar slenderBar = (SlenderBar) slenderBarSupplier.get();
        if (slenderBar == null) return;

        if (System.currentTimeMillis() < lastSoundTimeStamp) {
            player.playSound(ABORT_SOUND);
            return;
        }
        lastSoundTimeStamp = System.currentTimeMillis() + COOLDOWN_TIME;
        if (slenderBar.changeStatus()) {
            this.changeVisibilityStatus(player);
        }
    }

    /**
     * Changes the visibility status of the player.
     *
     * @param player the player to change the visibility status
     */
    private void changeVisibilityStatus(Player player) {
        Byte value = player.getTag(Tags.HIDDEN);
        byte currentValue = value != null ? value : SlenderBarHelper.VISIBLE;
        byte newValue = currentValue == SlenderBarHelper.VISIBLE ? SlenderBarHelper.HIDDEN : SlenderBarHelper.VISIBLE;
        player.setTag(Tags.HIDDEN, newValue);
        EventDispatcher.call(new SlenderVisibilityChangeEvent(player, newValue == SlenderBarHelper.HIDDEN));
    }
}
