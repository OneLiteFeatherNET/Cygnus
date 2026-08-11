package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composes the overlay layers into the title.
 * <p>
 * The title is the only HUD channel that renders centred and at four times scale, which is what
 * makes the overlays sit in the middle of the screen whatever the client's resolution is. It holds
 * one component, so the layers are concatenated with a negative spacer between them and end up
 * drawn on top of each other.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TitleScreenOverlay implements ScreenOverlay {

    /**
     * How long a frame survives without a follow-up. Long enough that the overlay never blinks
     * between updates, short enough that it disappears on its own if the server stops drawing.
     */
    private static final Title.Times TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ZERO
    );

    private final Map<UUID, Map<OverlayLayer, Component>> layers = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> timed = ConcurrentHashMap.newKeySet();

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(Player player, OverlayLayer layer, @Nullable Component glyph) {
        Map<OverlayLayer, Component> current = this.layers
                .computeIfAbsent(player.getUuid(), _ -> new EnumMap<>(OverlayLayer.class));

        if (glyph == null) {
            current.remove(layer);
        } else {
            current.put(layer, glyph);
        }

        this.send(player, current);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(Player player) {
        this.layers.remove(player.getUuid());
        this.timed.remove(player.getUuid());
        player.sendTitlePart(TitlePart.TITLE, Component.empty());
    }

    /**
     * Builds the title out of the player's layers and sends it.
     *
     * @param player  the player to draw for
     * @param current the layers currently set for them
     */
    private void send(Player player, Map<OverlayLayer, Component> current) {
        if (current.isEmpty()) {
            player.sendTitlePart(TitlePart.TITLE, Component.empty());
            return;
        }

        this.ensureTimes(player);

        Component composed = Component.empty();
        boolean first = true;
        // EnumMap iterates in declaration order, which is the drawing order of the layers.
        for (Component glyph : current.values()) {
            if (!first) composed = composed.append(OverlayFont.spacer());
            composed = composed.append(glyph);
            first = false;
        }
        player.sendTitlePart(TitlePart.TITLE, composed);
    }

    /**
     * Sends the fade timings once per player.
     * <p>
     * They stay in effect for every following title, so repeating them ten times a second would
     * only double the packet count.
     * </p>
     *
     * @param player the player to prepare
     */
    private void ensureTimes(Player player) {
        if (!this.timed.add(player.getUuid())) return;
        player.sendTitlePart(TitlePart.TIMES, TIMES);
    }
}
