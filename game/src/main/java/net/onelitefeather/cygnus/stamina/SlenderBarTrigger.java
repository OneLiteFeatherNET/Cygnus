package net.onelitefeather.cygnus.stamina;

import net.onelitefeather.cygnus.team.TeamHelper;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
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
        // A successful status change flips Tags.HIDDEN and fires the StaminaStateChangeEvent, and
        // StaminaStateChangeListener re-evaluates the viewable rule from there. Doing it a second time here
        // would be the exact kind of competing second update path this class used to carry.
        if (slenderBar.changeStatus()) return;
        player.playSound(ABORT_SOUND);
    }
}
