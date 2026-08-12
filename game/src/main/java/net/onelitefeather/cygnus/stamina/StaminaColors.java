package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

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

    private static final String HALF_TILE_CHAR = "▍";

    private final NamedTextColor completeColor;
    private final NamedTextColor emptyColor;

    /**
     * Creates a new instance of the StaminaColors
     *
     * @param completeColor the color for the complete bar
     * @param emptyColor    the color for the empty bar
     */
    StaminaColors(NamedTextColor completeColor, NamedTextColor emptyColor) {
        this.completeColor = completeColor;
        this.emptyColor = emptyColor;
    }

    /**
     * Sends a progress bar to the player. A dangling half stamina unit is rendered as its own half tile
     * instead of being rounded away, so the bar keeps the same width ({@code maxTime} tiles) while still
     * reflecting every half-step change.
     *
     * @param player      the player to send the progress bar
     * @param tileChar    the character to use for a full tile
     * @param currentTime the current time to display
     * @param maxTime     the maximum time the bar represents
     */
    public void sendProgressBar(Player player, String tileChar, double currentTime, int maxTime) {
        int fullTiles = (int) currentTime;
        boolean hasHalfTile = currentTime - fullTiles >= 0.5;
        int emptyTiles = maxTime - fullTiles - (hasHalfTile ? 1 : 0);

        Component progressBar = Component.text(tileChar.repeat(fullTiles), this.completeColor);
        if (hasHalfTile) {
            progressBar = progressBar.append(Component.text(HALF_TILE_CHAR, this.completeColor));
        }
        progressBar = progressBar.append(Component.text(tileChar.repeat(emptyTiles), this.emptyColor));
        player.sendActionBar(progressBar);
    }

    /**
     * Returns the color for the complete bar
     *
     * @return the color complete bar color
     */
    public NamedTextColor getCompleteColor() {
        return this.completeColor;
    }

    /**
     * Returns the color for the empty bar
     *
     * @return the color empty bar color
     */
    public NamedTextColor getEmptyColor() {
        return this.emptyColor;
    }
}
