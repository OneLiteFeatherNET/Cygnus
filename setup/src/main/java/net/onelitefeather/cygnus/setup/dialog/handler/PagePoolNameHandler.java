package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.guira.SetupDataService;

import java.util.Locale;

/**
 * Creates a page pool from the name entered in the dialog.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PagePoolNameHandler implements DialogHandler {

    private final SetupDataService dataService;

    public PagePoolNameHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        if (!(payload.get("name") instanceof StringBinaryTag nameTag)) return;
        Player player = event.getPlayer();
        String name = nameTag.value().trim();
        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;
            switch (gameData.createPagePool(name)) {
                case CREATED -> {
                    player.sendMessage(SetupMessages.getPagePoolCreated(name.toLowerCase(Locale.ROOT)));
                    SetupItems.setPagePoolEditItems(player);
                }
                case INVALID_NAME -> player.sendMessage(SetupMessages.getInvalidPagePoolName(name));
                case DUPLICATE -> player.sendMessage(SetupMessages.getDuplicatePagePool(name));
            }
        });
    }
}
