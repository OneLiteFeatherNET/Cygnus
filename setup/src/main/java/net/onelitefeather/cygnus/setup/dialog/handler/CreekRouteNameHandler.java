package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.guira.SetupDataService;

/**
 * Creates a creek route from the name entered in the dialog.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteNameHandler implements DialogHandler {

    private final SetupDataService dataService;

    public CreekRouteNameHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        if (!(payload.get("name") instanceof StringBinaryTag nameTag)) return;
        Player player = event.getPlayer();
        String name = nameTag.value().trim();
        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;
            if (name.isEmpty()) {
                player.sendMessage(SetupMessages.EMPTY_NAME);
                return;
            }
            if (!gameData.createCreekRoute(name)) {
                player.sendMessage(SetupMessages.getDuplicateCreekRoute(name));
                return;
            }
            player.sendMessage(SetupMessages.getCreekRouteCreated(name));
        });
    }
}
