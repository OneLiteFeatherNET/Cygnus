package net.onelitefeather.cygnus.setup.listener.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.dialogs.MapDialogs;
import net.onelitefeather.cygnus.setup.util.SetupData;

import java.util.function.Consumer;

public class DialogPayloadListener implements Consumer<PlayerCustomClickEvent> {

    private final SetupData setupData;

    public DialogPayloadListener(SetupData  setupData) {
        this.setupData = setupData;
    }

    @Override
    public void accept(PlayerCustomClickEvent event) {
        Key key = event.getKey();
        BinaryTag payload = event.getPayload();

        if (payload == null) return;

        CompoundBinaryTag castedPayload = (CompoundBinaryTag) payload;

        if (key.equals(MapDialogs.MAP_KEY)) {
            StringBinaryTag nameBinary = (StringBinaryTag) castedPayload.get("name");
            String nameEntry = nameBinary.value();

            if (nameEntry.trim().isBlank()) {
                return;
            }


            this.setupData.getBaseMapBuilder().name(nameEntry);
        }
    }
}
