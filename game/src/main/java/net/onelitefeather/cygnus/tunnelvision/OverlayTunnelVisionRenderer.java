package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.ScreenOverlay;

/**
 * Contributes the tunnel vision to the shared screen overlay.
 * <p>
 * Each stage is a camera overlay texture from the resource pack. Minecraft cannot animate one, so
 * the heartbeat is the server walking through the stages, one texture per frame.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
public final class OverlayTunnelVisionRenderer implements TunnelVisionRenderer {

    /** Where the stage textures live, as {@code camera_overlay} resolves them. */
    static final String TEXTURE_PATH = "gui/tunnel_vision/stage_";

    private static final Key[] TEXTURES = buildTextures();

    private final ScreenOverlay overlay;

    /**
     * Creates a renderer drawing into the given overlay.
     *
     * @param overlay the overlay that owns the player's screen
     */
    public OverlayTunnelVisionRenderer(ScreenOverlay overlay) {
        this.overlay = overlay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Player player, int stage) {
        if (stage <= 0) {
            this.clear(player);
            return;
        }
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TEXTURES[Math.min(stage, TunnelVisionStage.MAX_STAGE) - 1]);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only this layer is dropped. Clearing the screen would take the blood splatter with it.
     * </p>
     */
    @Override
    public void clear(Player player) {
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, null);
    }

    /**
     * Builds the texture key for every stage.
     *
     * @return the keys, indexed by {@code stage - 1}
     */
    private static Key[] buildTextures() {
        Key[] textures = new Key[TunnelVisionStage.MAX_STAGE];
        for (int stage = 1; stage <= TunnelVisionStage.MAX_STAGE; stage++) {
            textures[stage - 1] = Key.key("cygnus", TEXTURE_PATH + stage);
        }
        return textures;
    }
}
