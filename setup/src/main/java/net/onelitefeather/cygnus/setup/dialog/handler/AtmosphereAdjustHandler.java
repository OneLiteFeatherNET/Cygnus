package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.guira.SetupDataService;

/**
 * Sends a builder from a running preview back to the value dialog.
 *
 * <p>The preview keeps running while they adjust, so they are comparing the new numbers against the
 * fog they are standing in rather than against the setup hub.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmosphereAdjustHandler implements DialogHandler {

    private final SetupDataService dataService;

    /**
     * Creates a new handler.
     *
     * @param dataService the service holding the setup of each builder
     */
    public AtmosphereAdjustHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        this.dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;

            MapAtmosphere atmosphere = ((GameMapBuilder) gameData.getMapBuilder()).getAtmosphere();
            if (atmosphere == null) return;

            AtmosphereDialogs.openValueDialog(event.getPlayer(), atmosphere);
        });
    }
}
