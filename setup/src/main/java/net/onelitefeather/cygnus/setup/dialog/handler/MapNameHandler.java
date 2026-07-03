package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.guira.SetupDataService;

public final class MapNameHandler implements DialogHandler{
    private final SetupDataService dataService;

    public MapNameHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        StringBinaryTag nameBinary = (StringBinaryTag) payload.get("name");
        String nameEntry = nameBinary.value();
        if (nameEntry.trim().isBlank()) {
            return;
        }
        dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            InstanceSetupData instance = (InstanceSetupData) data;

            if (instance.getMapBuilder().getName().equalsIgnoreCase(nameEntry)) return;

            instance.getMapBuilder().name(nameEntry);
            instance.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
            instance.updateTitle();
        });
    }
}
