package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.jetbrains.annotations.Nullable;

/**
 * Event will be fired during the setup when a player wants to delete data from a map.
 *
 * @author Joltra
 * @version 1.1.0
 * @since 2.5.1
 */
public final class PlayerRemoveDataEvent implements PlayerEvent {

    private final Player player;
    private final MapDataCategory mapDataCategory;
    private final @Nullable DialogContext context;

    /**
     * Creates a new instance of the event with the given values from the constructor
     *
     * @param player          which is involved
     * @param mapDataCategory which data should be deleted
     * @param context         if some data needs to be passed as a context
     */
    public PlayerRemoveDataEvent(Player player, MapDataCategory mapDataCategory, @Nullable DialogContext context) {
        this.player = player;
        this.mapDataCategory = mapDataCategory;
        this.context = context;
    }

    /**
     * Creates a new instance of the event with the given values from the constructor
     *
     * @param player          which is involved
     * @param mapDataCategory which data should be deleted
     */
    public PlayerRemoveDataEvent(Player player, MapDataCategory mapDataCategory) {
        this.player = player;
        this.mapDataCategory = mapDataCategory;
        this.context = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns the involved category.
     *
     * @return the given category
     */
    public MapDataCategory getMapDataCategory() {
        return mapDataCategory;
    }

    /**
     * Returns the additional context which could be involved.
     *
     * @return additional context data
     */
    public @Nullable DialogContext getContext() {
        return context;
    }
}
