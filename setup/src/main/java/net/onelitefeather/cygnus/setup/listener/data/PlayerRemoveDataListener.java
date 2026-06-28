package net.onelitefeather.cygnus.setup.listener.data;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.dialogs.MapDialogs;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;

import java.util.function.Consumer;

public final class PlayerRemoveDataListener implements Consumer<PlayerRemoveDataEvent> {
    @Override
    public void accept(PlayerRemoveDataEvent event) {
        final Player player = event.getPlayer();
        final MapDataCategory category = event.getMapDataCategory();

        MapDialogs.openDeleteDialog(player, category);
    }
}
