package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;

/**
 * Event will be fired during the setup when a player wants to delete data from a map.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.5.1
 */
public final class PlayerRemoveDataEvent implements PlayerEvent {

    private final Player player;
    private final MapDataCategory mapDataCategory;

    /**
     * Creates a new instance of the event with the given values from the constructor
     *
     * @param player          which is involved
     * @param mapDataCategory which data should be deleted
     */
    public PlayerRemoveDataEvent(Player player, MapDataCategory mapDataCategory) {
        this.player = player;
        this.mapDataCategory = mapDataCategory;
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
}
