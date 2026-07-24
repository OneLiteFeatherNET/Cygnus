package net.onelitefeather.cygnus.setup.player;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link Player} implementation to add additional context related to the setup flow.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.0.0
 */
public final class SetupPlayer extends Player {

    private @Nullable Point survivorToDelete;
    private @Nullable Point pageToDelete;

    /**
     * {@inheritDoc}
     */
    public SetupPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    /**
     * Marks a survivor for deletion.
     *
     * @param survivor position to delete
     */
    public void setSurvivorToDelete(@Nullable Point survivor) {
        this.survivorToDelete = survivor;
    }

    /**
     * Marks a page for deletion.
     *
     * @param page position to delete
     */
    public void setPageToDelete(@Nullable Point page) {
        this.pageToDelete = page;
    }

    /**
     * Returns the survivor marked for deletion.
     *
     * @return selected survivor position or {@code null}
     */
    public @Nullable Point getSurvivorToDelete() {
        return survivorToDelete;
    }

    /**
     * Returns the page marked for deletion.
     *
     * @return selected page position or {@code null}
     */
    public @Nullable Point getPageToDelete() {
        return pageToDelete;
    }
}
