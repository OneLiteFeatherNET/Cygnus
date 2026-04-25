package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.setup.util.SetupData;

/**
 * Called when a {@link Player} wants to save the data from a map.
 *
 * @version 1.0.0
 * @since 2.1.0
 * @author theEvilReaper
 */
public final class MapSetupSaveEvent implements PlayerEvent {

    private final Player player;
    private final SetupData setupData;

    /**
     * Creates a new instance of the event with the given parameters.
     *
     * @param player    which is involved
     * @param setupData which contains the data to save
     */
    public MapSetupSaveEvent(Player player, SetupData setupData) {
        this.player = player;
        this.setupData = setupData;
    }

    /**
     * Returns the {@link SetupData} associated with this event.
     *
     * @return associated data
     */
    public SetupData getSetupData() {
        return this.setupData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }
}
