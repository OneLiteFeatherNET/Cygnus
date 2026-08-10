package net.onelitefeather.cygnus.tunnelvision;

import net.minestom.server.entity.Player;

/**
 * Displays a tunnel vision stage to a survivor.
 * <p>
 * This is the seam between the game logic and the way the effect reaches the screen. Minecraft
 * 26.2 offers no per-player post-processing effect, so the only implementation today draws the
 * vignette as a HUD overlay. Once {@code /posteffect} is available a second implementation can
 * take its place without the calculation or the service noticing.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public interface TunnelVisionRenderer {

    /**
     * Shows the given stage to the player.
     *
     * @param player the player to draw for
     * @param stage  the stage between {@code 0} and {@link TunnelVisionStage#MAX_STAGE}, where
     *               {@code 0} means no overlay
     */
    void render(Player player, int stage);

    /**
     * Removes the overlay from the player's screen.
     *
     * @param player the player to clear
     */
    void clear(Player player);
}
