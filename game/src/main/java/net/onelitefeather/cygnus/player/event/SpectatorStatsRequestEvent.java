package net.onelitefeather.cygnus.player.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a spectator asks for the statistics of a player from the spectate overview.
 * <p>
 * The overview only says what was asked for. Whoever shows statistics listens for it, so the
 * spectator code does not have to know whether statistics are there at all.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.19.0
 */
public final class SpectatorStatsRequestEvent implements PlayerEvent {

    private final Player player;
    private final Player target;

    /**
     * Creates the event.
     *
     * @param player the spectator who asked
     * @param target the player whose statistics they want to see
     */
    public SpectatorStatsRequestEvent(Player player, Player target) {
        this.player = player;
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the player whose statistics were asked for.
     *
     * @return the target
     */
    public Player getTarget() {
        return target;
    }
}
