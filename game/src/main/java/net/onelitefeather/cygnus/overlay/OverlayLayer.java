package net.onelitefeather.cygnus.overlay;

/**
 * The full-screen layers a player can have on their HUD at once, in drawing order — later
 * constants are drawn on top of earlier ones.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public enum OverlayLayer {

    /** The narrowing view, driven by stamina and the slender's presence. */
    TUNNEL_VISION,

    /** The splatter that flashes up when the player is hit; sits closest to the eye. */
    BLOOD
}
