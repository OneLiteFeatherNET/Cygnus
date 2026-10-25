package net.onelitefeather.cygnus.player.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called when a spectator asks to be teleported to a player from the spectate overview.
 * <p>
 * The overview only says where the spectator wants to go. Whoever moves spectators listens for it,
 * so the overview does not have to carry the teleport logic around.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.20.0
 */
public final class SpectatorTeleportRequestEvent implements PlayerEvent {

    private final Player player;
    private final Player target;

    /**
     * Creates the event.
     *
     * @param player the spectator who asked
     * @param target the player they want to be teleported to
     */
    public SpectatorTeleportRequestEvent(Player player, Player target) {
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
     * Returns the player the spectator wants to be teleported to.
     *
     * @return the target
     */
    public Player getTarget() {
        return target;
    }
}
