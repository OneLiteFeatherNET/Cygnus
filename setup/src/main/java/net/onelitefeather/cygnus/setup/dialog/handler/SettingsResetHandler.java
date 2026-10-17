package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData.InventoryTarget;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForm;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.guira.SetupDataService;

/**
 * Puts the settings of a map back to their defaults when the reset button of their dialog is used.
 *
 * @param <S> the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SettingsResetHandler<S> implements DialogHandler {

    private final SetupDataService dataService;
    private final SettingsForm<S> form;

    /**
     * Creates the handler.
     *
     * @param dataService where the player's setup data lives
     * @param form        the settings this handler resets
     */
    public SettingsResetHandler(SetupDataService dataService, SettingsForm<S> form) {
        this.dataService = dataService;
        this.form = form;
    }

    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        Player player = event.getPlayer();
        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;
            this.form.set().accept((GameMapBuilder) gameData.getMapBuilder(), this.form.defaults());
            gameData.triggerUpdate(InventoryTarget.GENERAL);
            player.sendMessage(SetupMessages.getSettingsReset(this.form.title()));
        });
    }
}
