package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the HUD channel that carries the full-screen overlays and composes the layers into it.
 * <p>
 * Both effects live in the same title, so neither can send one on its own without wiping the
 * other. They hand their glyph here instead and this decides what ends up on screen.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public interface ScreenOverlay {

    /**
     * Sets or removes what a layer contributes to the player's screen.
     *
     * @param player the player to draw for
     * @param layer  the layer to change
     * @param glyph  the glyph to show, or {@code null} to drop the layer
     */
    void set(Player player, OverlayLayer layer, @Nullable Component glyph);

    /**
     * Drops every layer and clears the player's screen.
     *
     * @param player the player to clear
     */
    void clear(Player player);
}
