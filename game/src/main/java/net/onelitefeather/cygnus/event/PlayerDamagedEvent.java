package net.onelitefeather.cygnus.event;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a player takes damage from the game.
 * <p>
 * Cygnus applies damage through {@code Vitality}, which sets health directly and so never raises
 * Minestom's {@code EntityDamageEvent}. This event fills that gap for everything that needs to
 * react to a hit — the blood splatter above all — and carries where the hit came from, so the
 * reaction can be aimed.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.7.0
 */
@SuppressWarnings("java:S6206")
public final class PlayerDamagedEvent implements PlayerEvent {

    private final Player player;
    private final Point source;

    /**
     * Creates a new instance of the {@link PlayerDamagedEvent}.
     *
     * @param player the player who was hit
     * @param source where the damage came from
     */
    public PlayerDamagedEvent(Player player, Point source) {
        this.player = player;
        this.source = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns where the damage came from.
     *
     * @return the position of the source
     */
    public Point getSource() {
        return this.source;
    }
}
