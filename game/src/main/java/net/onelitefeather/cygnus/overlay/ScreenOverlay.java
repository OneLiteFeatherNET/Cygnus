package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the full-screen overlay of a player and decides what ends up on it.
 * <p>
 * The effects hand over a texture for their layer rather than drawing themselves, because a player
 * only has one screen to give: whichever effect drew last would otherwise wipe the other.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
public interface ScreenOverlay {

    /**
     * Sets or removes what a layer contributes to the player's screen.
     *
     * @param player  the player to draw for
     * @param layer   the layer to change
     * @param texture the overlay texture to show, or {@code null} to drop the layer
     */
    void set(Player player, OverlayLayer layer, @Nullable Key texture);

    /**
     * Drops every layer and clears the player's screen.
     *
     * @param player the player to clear
     */
    void clear(Player player);
}
