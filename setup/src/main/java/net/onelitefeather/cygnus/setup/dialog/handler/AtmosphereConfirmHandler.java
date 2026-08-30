package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.atmosphere.AtmospherePreviewService;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.guira.SetupDataService;

/**
 * Ends a preview by keeping the values it was rendering.
 *
 * <p>The values are already on the map builder - {@link AtmosphereValuesHandler} put them there
 * before the preview started - so all that is left here is bringing the builder home and showing
 * them the inventory again. Declining the dialog instead leaves the builder in the preview until
 * they adjust the values or walk out of it.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmosphereConfirmHandler implements DialogHandler {

    private final SetupDataService dataService;
    private final AtmospherePreviewService previewService;

    /**
     * Creates a new handler.
     *
     * @param dataService    the service holding the setup of each builder
     * @param previewService the service that owns the running preview
     */
    public AtmosphereConfirmHandler(SetupDataService dataService, AtmospherePreviewService previewService) {
        this.dataService = dataService;
        this.previewService = previewService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        this.previewService.leave(event.getPlayer());

        this.dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            InstanceSetupData setupData = (InstanceSetupData) data;
            setupData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
        });
    }
}
