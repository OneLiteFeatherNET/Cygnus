package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.guira.SetupDataService;

public final class NonDynamicDataHandler implements DialogHandler {

    private final SetupDataService dataService;

    public NonDynamicDataHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        IntBinaryTag idTag = (IntBinaryTag) payload.get("category_id");
        if (idTag == null) return;
        int categoryId = idTag.value();
        MapDataCategory category = MapDataCategory.byId(categoryId);

        dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            ((InstanceSetupData)data).handleDataDelete(category);
        });
    }
}
