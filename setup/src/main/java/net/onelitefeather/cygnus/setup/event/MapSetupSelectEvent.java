package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.theevilreaper.aves.map.MapEntry;

import java.util.function.BiFunction;

/**
 * The {@link MapSetupSelectEvent} is called when a player selects a map for the setup process.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public final class MapSetupSelectEvent implements PlayerEvent, CancellableEvent {

    private final Player player;
    private final MapEntry mapEntry;
    private final BiFunction<Player, MapEntry, InstanceSetupData> dataFactory;
    private boolean cancelled;

    /**
     * Creates a new instance of the {@link MapSetupSelectEvent}.
     *
     * @param player    the player who selected the map
     * @param mapEntry  the selected map
     * @param dataFactory creates the setup data for the chosen mode, for example {@code LobbyData::new}
     */
    public MapSetupSelectEvent(Player player, MapEntry mapEntry, BiFunction<Player, MapEntry, InstanceSetupData> dataFactory) {
        this.player = player;
        this.mapEntry = mapEntry;
        this.dataFactory = dataFactory;
    }

    /**
     * Checks if the event is cancelled.
     *
     * @return true if the event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets the cancellation state for the event.
     *
     * @param cancel the new state for the event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Returns the selected map entry.
     *
     * @return the map entry
     */
    public MapEntry getMapEntry() {
        return mapEntry;
    }

    /**
     * Creates the setup data for the selected map in the chosen mode.
     *
     * @return the new setup data
     */
    public InstanceSetupData createData() {
        return this.dataFactory.apply(this.player, this.mapEntry);
    }

    /**
     * Returns the player who selected the map.
     *
     * @return the player
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }
}
