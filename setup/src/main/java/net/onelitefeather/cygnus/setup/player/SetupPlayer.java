package net.onelitefeather.cygnus.setup.player;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link Player} implementation to add additional context related to the setup flow.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 2.0.0
 */
public final class SetupPlayer extends Player {

    private @Nullable Point survivorToDelete;
    private @Nullable Point pageToDelete;
    private @Nullable PageResource pageResource;

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
     * Sets the page resource which should be used deleted.
     *
     * @param pageResource the page resource to set
     */
    public void setPageResource(@Nullable PageResource pageResource) {
        this.pageResource = pageResource;
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

    /**
     * Returns the page resource which should be used deleted.
     *
     * @return the page resource to delete
     */
    public @Nullable PageResource getPageResource() {
        return pageResource;
    }
}
