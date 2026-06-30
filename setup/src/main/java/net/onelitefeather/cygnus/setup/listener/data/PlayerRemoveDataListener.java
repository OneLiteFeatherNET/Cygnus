package net.onelitefeather.cygnus.setup.listener.data;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;

import java.util.function.Consumer;

public final class PlayerRemoveDataListener implements Consumer<PlayerRemoveDataEvent> {

    @Override
    public void accept(PlayerRemoveDataEvent event) {
        final Player player = event.getPlayer();
        final MapDataCategory category = event.getMapDataCategory();

        if (event.getContext() == null || (!(category == MapDataCategory.SURVIVOR ||  category == MapDataCategory.PAGE))) {
            MapDialogs.openDeleteDialog(player, category);
            return;
        }


        DialogContext context = event.getContext();
        SetupPlayer setupPlayer = (SetupPlayer) player;

        if (context instanceof DialogContext.PositionContent positionContent) {
           setupPlayer.setSurvivorToDelete(positionContent.point());
        }

        if (context instanceof DialogContext.PageContent pageContent) {
            setupPlayer.setPageResource(pageContent.resource());
        }

        MapDialogs.openDeleteDialog(player, category, context);
    }
}
