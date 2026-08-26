package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link ScreenOverlay} that records what was set instead of dressing a player.
 * <p>
 * Every effect that draws onto the screen needs the same thing from a test: which texture ended up
 * on which layer, without a real equipment slot in the way. Written per test class, that is the same
 * {@code Map<UUID, EnumMap<OverlayLayer, Key>>} five times over - the very duplication the overlay
 * foundation removes from the production code. It lives here rather than beside any one effect
 * because no effect owns it.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RecordingScreenOverlay overlay = new RecordingScreenOverlay();
 * service.show(player, 3);
 * assertEquals(expected, overlay.of(player, OverlayLayer.TUNNEL_VISION));
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class RecordingScreenOverlay implements ScreenOverlay {

    private final Map<UUID, Map<OverlayLayer, Key>> layers = new HashMap<>();

    @Override
    public void set(Player player, OverlayLayer layer, @Nullable Key texture) {
        Map<OverlayLayer, Key> current =
                this.layers.computeIfAbsent(player.getUuid(), key -> new EnumMap<>(OverlayLayer.class));
        if (texture == null) {
            current.remove(layer);
            return;
        }
        current.put(layer, texture);
    }

    @Override
    public void clear(Player player) {
        this.layers.remove(player.getUuid());
    }

    /**
     * Reads back what a layer currently holds for a player.
     *
     * @param player the player to look up
     * @param layer  the layer to look up
     * @return the texture currently set, or {@code null} if there is none
     */
    public @Nullable Key of(Player player, OverlayLayer layer) {
        return this.layers.getOrDefault(player.getUuid(), Map.of()).get(layer);
    }

    /**
     * @param player the player to look up
     * @return {@code true} if no layer is currently set for that player
     */
    public boolean isEmpty(Player player) {
        return this.layers.getOrDefault(player.getUuid(), Map.of()).isEmpty();
    }
}
