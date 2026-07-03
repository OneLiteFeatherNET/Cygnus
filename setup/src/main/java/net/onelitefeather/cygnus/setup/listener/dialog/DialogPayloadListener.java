package net.onelitefeather.cygnus.setup.listener.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
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

    public DialogPayloadListener(SetupDataService dataService) {
        this.handlers = Map.ofEntries(
                Map.entry(MapDialogs.MAP_KEY, new MapNameHandler(dataService)),
                Map.entry(MapDialogs.AUTHOR_AMOUNT_KEY, new AuthorAmountHandler()),
                Map.entry(MapDialogs.AUTHOR_INPUT_ENTRY_KEY, new AuthorInputHandler(dataService)),
                Map.entry(MapDialogs.NON_DYNAMIC_DELETE_KEY, new NonDynamicDataHandler(dataService)),
                Map.entry(MapDialogs.DYNAMIC_DELETE_KEY, new DynamicDataHandler(dataService))
        );
    }

    @Override
    public void accept(PlayerCustomClickEvent event) {
        Key key = event.getKey();
        BinaryTag payload = event.getPayload();
        if (payload == null) return;

        CompoundBinaryTag castedPayload = (CompoundBinaryTag) payload;
        this.handlers.get(key).handle(event, castedPayload);
    }
}
