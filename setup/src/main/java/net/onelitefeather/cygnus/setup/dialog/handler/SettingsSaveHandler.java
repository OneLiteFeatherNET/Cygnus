package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData.InventoryTarget;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsDialog;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForm;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.guira.SetupDataService;

import java.util.Map;

/**
 * Stores the values of a settings dialog on the map. Values that do not fit together are not
 * stored; the dialog opens again with what the player entered, so nothing has to be set twice.
 *
 * @param <S> the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SettingsSaveHandler<S> implements DialogHandler {

    private final SetupDataService dataService;
    private final SettingsForm<S> form;

    /**
     * Creates the handler.
     *
     * @param dataService where the player's setup data lives
     * @param form        the settings this handler stores
     */
    public SettingsSaveHandler(SetupDataService dataService, SettingsForm<S> form) {
        this.dataService = dataService;
        this.form = form;
    }

    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        Player player = event.getPlayer();
        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;
            GameMapBuilder builder = (GameMapBuilder) gameData.getMapBuilder();
            Map<String, Double> entered = this.form.read(payload, this.form.values(this.form.get().apply(builder)));
            S settings;
            try {
                settings = this.form.apply(entered);
            } catch (IllegalArgumentException exception) {
                player.sendMessage(SetupMessages.getInvalidSettings(exception.getMessage()));
                SettingsDialog.open(player, this.form, entered);
                return;
            }
            this.form.set().accept(builder, settings);
            gameData.triggerUpdate(InventoryTarget.GENERAL);
            player.sendMessage(SetupMessages.getSettingsSaved(this.form.title()));
        });
    }
}
