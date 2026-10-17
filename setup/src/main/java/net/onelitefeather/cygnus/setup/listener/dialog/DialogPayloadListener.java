package net.onelitefeather.cygnus.setup.listener.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.atmosphere.AtmospherePreviewService;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.cygnus.setup.dialog.CreekRouteDialogs;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
import net.onelitefeather.cygnus.setup.dialog.handler.CreekRouteNameHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.CreekRoutePauseHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AtmosphereAdjustHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AtmosphereConfirmHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AtmospherePresetHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AtmosphereValuesHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AuthorAmountHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.AuthorInputHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.DialogHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.DynamicDataHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.MapNameHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.NonDynamicDataHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.SettingsResetHandler;
import net.onelitefeather.cygnus.setup.dialog.handler.SettingsSaveHandler;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForms;
import net.onelitefeather.guira.SetupDataService;

import java.util.Map;
import java.util.function.Consumer;

public class DialogPayloadListener implements Consumer<PlayerCustomClickEvent> {

    private final Map<Key, DialogHandler> handlers;

    public DialogPayloadListener(SetupDataService dataService, AtmospherePreviewService previewService) {
        this.handlers = Map.ofEntries(
                Map.entry(MapDialogs.MAP_KEY, new MapNameHandler(dataService)),
                Map.entry(CreekRouteDialogs.ROUTE_NAME_KEY, new CreekRouteNameHandler(dataService)),
                Map.entry(CreekRouteDialogs.PAUSE_KEY, new CreekRoutePauseHandler(dataService)),
                Map.entry(MapDialogs.AUTHOR_AMOUNT_KEY, new AuthorAmountHandler()),
                Map.entry(MapDialogs.AUTHOR_INPUT_ENTRY_KEY, new AuthorInputHandler(dataService)),
                Map.entry(MapDialogs.NON_DYNAMIC_DELETE_KEY, new NonDynamicDataHandler(dataService)),
                Map.entry(MapDialogs.DYNAMIC_DELETE_KEY, new DynamicDataHandler(dataService)),
                Map.entry(AtmosphereDialogs.PRESET_KEY, new AtmospherePresetHandler(dataService)),
                Map.entry(AtmosphereDialogs.VALUES_KEY, new AtmosphereValuesHandler(dataService, previewService)),
                Map.entry(AtmosphereDialogs.CONFIRM_KEY, new AtmosphereConfirmHandler(dataService, previewService)),
                Map.entry(AtmosphereDialogs.ADJUST_KEY, new AtmosphereAdjustHandler(dataService)),
                Map.entry(SettingsForms.PAGES.saveKey(), new SettingsSaveHandler<>(dataService, SettingsForms.PAGES)),
                Map.entry(SettingsForms.PAGES.resetKey(), new SettingsResetHandler<>(dataService, SettingsForms.PAGES)),
                Map.entry(SettingsForms.CREEK.saveKey(), new SettingsSaveHandler<>(dataService, SettingsForms.CREEK)),
                Map.entry(SettingsForms.CREEK.resetKey(), new SettingsResetHandler<>(dataService, SettingsForms.CREEK))
        );
    }

    @Override
    public void accept(PlayerCustomClickEvent event) {
        Key key = event.getKey();
        BinaryTag payload = event.getPayload();
        if (payload == null) return;

        DialogHandler handler = this.handlers.get(key);
        if (handler == null) return;

        CompoundBinaryTag castedPayload = (CompoundBinaryTag) payload;
        handler.handle(event, castedPayload);
    }
}
