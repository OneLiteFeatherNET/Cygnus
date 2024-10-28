package net.onelitefeather.cygnus.stamina;

import de.icevizion.aves.util.functional.PlayerConsumer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.utils.TeamHelper;
import org.jetbrains.annotations.NotNull;
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
public final class SlenderBarTrigger {

    private final Supplier<@Nullable StaminaBar> slenderBarSupplier;
    private final PlayerConsumer updateRuneFunction;

    /**
     * Creates a new instance of this class.
     *
     * @param slenderBarSupplier the supplier to get the {@link SlenderBar}
     * @param updateRuneFunction the function to update the rune status
     */
    public SlenderBarTrigger(@NotNull Supplier<@Nullable StaminaBar> slenderBarSupplier, @NotNull PlayerConsumer updateRuneFunction) {
        this.slenderBarSupplier = slenderBarSupplier;
        this.updateRuneFunction = updateRuneFunction;
    }

    /**
     * Triggers the activation of the {@link SlenderBar} for the given {@link Player}.
     *
     * @param player the player to trigger the activation
     */
    public void trigger(@NotNull Player player) {
        if (!TeamHelper.isSlenderTeam(player)) return;
        SlenderBar slenderBar = (SlenderBar) slenderBarSupplier.get();
        if (slenderBar == null) return;

        if (slenderBar.changeStatus()) {
            this.changeVisibilityStatus(player);
            this.updateRuneFunction.accept(player);
        }
    }

    /**
     * Changes the visibility status of the player.
     *
     * @param player the player to change the visibility status
     */
    private void changeVisibilityStatus(@NotNull Player player) {
        var currentValue = player.getTag(Tags.HIDDEN);
        if (currentValue == 0) {
            player.setTag(Tags.HIDDEN, (byte) 1);
        } else {
            player.setTag(Tags.HIDDEN, (byte) 0);
        }
    }
}
