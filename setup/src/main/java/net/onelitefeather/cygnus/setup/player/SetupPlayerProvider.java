package net.onelitefeather.cygnus.setup.player;

import net.minestom.server.entity.Player;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;

/**
 * Implementation of the {@link PlayerProvider} interface to allow the creation of {@link SetupPlayer} references
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.0.0
 */
public final class SetupPlayerProvider implements PlayerProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Player createPlayer(PlayerConnection connection, GameProfile gameProfile) {
        return new SetupPlayer(connection, gameProfile);
    }
}
