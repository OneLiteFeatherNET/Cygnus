package net.onelitefeather.cygnus.overlay;

/**
 * Decides whether the full-screen overlays — the tunnel vision and the blood splatter — run.
 * <p>
 * They used to be tied to the ResourcePack feature, on the grounds that without the pack their
 * textures are missing and a player would get a fullscreen checkerboard. That was too blunt: a
 * server can be run without handing out a pack while the people testing it have the pack enabled
 * locally, and in that setup the effects silently never started.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class OverlayProperties {

    static final String ENABLED_PROPERTY = "cygnus.overlays";

    private OverlayProperties() {
    }

    /**
     * Tells whether the overlays should run.
     * <p>
     * On unless the property says {@code false}. Anything unreadable leaves them on: the effects
     * are part of the game, and a typo in a start script should not quietly remove them.
     * </p>
     *
     * @return whether to register the overlay services
     */
    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty(ENABLED_PROPERTY, "true").trim());
    }
}
