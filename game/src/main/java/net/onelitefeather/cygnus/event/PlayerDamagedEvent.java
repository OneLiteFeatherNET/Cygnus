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
 * @version 1.2.0
 * @since 2.7.0
 */
@SuppressWarnings("java:S6206")
public final class PlayerDamagedEvent implements PlayerEvent {

    private final Player player;
    private final Point source;
    private final double points;

    /**
     * Creates a new instance of the {@link PlayerDamagedEvent} for a hit whose size is not known.
     *
     * @param player the player who was hit
     * @param source where the damage came from
     */
    public PlayerDamagedEvent(Player player, Point source) {
        this(player, source, 0.0D);
    }

    /**
     * Creates a new instance of the {@link PlayerDamagedEvent}.
     *
     * @param player the player who was hit
     * @param source where the damage came from
     * @param points the vitality points the hit took
     */
    public PlayerDamagedEvent(Player player, Point source, double points) {
        this.player = player;
        this.source = source;
        this.points = points;
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

    /**
     * Returns how much the hit took.
     *
     * @return the vitality points lost, {@code 0} when the sender does not know
     */
    public double getPoints() {
        return this.points;
    }
}
