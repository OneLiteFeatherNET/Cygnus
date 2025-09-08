package net.onelitefeather.cygnus.stamina;

import net.theevilreaper.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The StaminaColors enum is designed to provide different colors for the stamina bar.
 * The colors are used to display the current state of the stamina bar.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
enum StaminaColors {

    DRAINING(NamedTextColor.GOLD, NamedTextColor.GRAY),
    REGENERATING(NamedTextColor.GREEN, NamedTextColor.GRAY);

    private final NamedTextColor completeColor;
    private final NamedTextColor emptyColor;

    /**
     * Creates a new instance of the StaminaColors
     *
     * @param completeColor the color for the complete bar
     * @param emptyColor    the color for the empty bar
     */
    StaminaColors(@NotNull NamedTextColor completeColor, @NotNull NamedTextColor emptyColor) {
        this.completeColor = completeColor;
        this.emptyColor = emptyColor;
    }

    /**
     * Sends a progress bar to the player
     *
     * @param player      the player to send the progress bar
     * @param tileChar    the character to use for the progress bar
     * @param currentTime the current time to display
     */
    public void sendProgressBar(@NotNull Player player, @NotNull String tileChar, int currentTime) {
        Component progressBar = Components.getProgressBar(currentTime, 17, 17, tileChar, this.completeColor, this.emptyColor);
        player.sendActionBar(progressBar);
    }

    /**
     * Returns the color for the complete bar
     *
     * @return the color complete bar color
     */
    public @NotNull NamedTextColor getCompleteColor() {
        return this.completeColor;
    }

    /**
     * Returns the color for the empty bar
     *
     * @return the color empty bar color
     */
    public @NotNull NamedTextColor getEmptyColor() {
        return this.emptyColor;
    }
}
