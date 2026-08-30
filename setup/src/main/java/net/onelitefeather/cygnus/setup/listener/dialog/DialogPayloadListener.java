package net.onelitefeather.cygnus.setup.listener.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.atmosphere.AtmospherePreviewService;
import net.onelitefeather.cygnus.setup.dialog.AtmosphereDialogs;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
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
import net.onelitefeather.guira.SetupDataService;

import java.util.Map;
import java.util.function.Consumer;

public class DialogPayloadListener implements Consumer<PlayerCustomClickEvent> {

    private final Map<Key, DialogHandler> handlers;

    public DialogPayloadListener(SetupDataService dataService, AtmospherePreviewService previewService) {
        this.handlers = Map.ofEntries(
                Map.entry(MapDialogs.MAP_KEY, new MapNameHandler(dataService)),
                Map.entry(MapDialogs.AUTHOR_AMOUNT_KEY, new AuthorAmountHandler()),
                Map.entry(MapDialogs.AUTHOR_INPUT_ENTRY_KEY, new AuthorInputHandler(dataService)),
                Map.entry(MapDialogs.NON_DYNAMIC_DELETE_KEY, new NonDynamicDataHandler(dataService)),
                Map.entry(MapDialogs.DYNAMIC_DELETE_KEY, new DynamicDataHandler(dataService)),
                Map.entry(AtmosphereDialogs.PRESET_KEY, new AtmospherePresetHandler(dataService)),
                Map.entry(AtmosphereDialogs.VALUES_KEY, new AtmosphereValuesHandler(dataService, previewService)),
                Map.entry(AtmosphereDialogs.CONFIRM_KEY, new AtmosphereConfirmHandler(dataService, previewService)),
                Map.entry(AtmosphereDialogs.ADJUST_KEY, new AtmosphereAdjustHandler(dataService))
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
