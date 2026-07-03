package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.guira.SetupDataService;

public final class AuthorInputHandler implements DialogHandler {

    private final SetupDataService setupDataService;

    public AuthorInputHandler(SetupDataService setupDataService) {
        this.setupDataService = setupDataService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        FloatBinaryTag amountBinary = (FloatBinaryTag) payload.get("amount");
        if (amountBinary == null) return;

        float amount = amountBinary.value();

        String[] authors = new String[(int) amount];
        for (int i = 0; i < amount; i++) {
            authors[i] = payload.getString("author_" + i);
        }

        setupDataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            InstanceSetupData instanceSetupData = (InstanceSetupData) data;
            instanceSetupData.getMapBuilder().builders(authors);
            instanceSetupData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
        });
    }
}
