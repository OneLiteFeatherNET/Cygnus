package net.onelitefeather.cygnus.setup.listener.data;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.dialogs.MapDialogs;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public final class PlayerRemoveDataListener implements Consumer<PlayerRemoveDataEvent> {

    private static final Set<MapDataCategory> CONTEXT_AWARE_CATEGORIES =
            EnumSet.of(MapDataCategory.SURVIVOR, MapDataCategory.PAGE);

    @Override
    public void accept(PlayerRemoveDataEvent event) {
        final Player player = event.getPlayer();
        final MapDataCategory category = event.getMapDataCategory();
        final DialogContext context = event.getContext();

        if (context == null || !CONTEXT_AWARE_CATEGORIES.contains(category)) {
            MapDialogs.openDeleteDialog(player, category);
            return;
        }

        final SetupPlayer setupPlayer = (SetupPlayer) player;

        switch (context) {
            case DialogContext.PositionContent(Point point) -> setupPlayer.setSurvivorToDelete(point);
            case DialogContext.PageContent(PageResource resource) -> setupPlayer.setPageResource(resource);
            default -> { }
        }

        MapDialogs.openDeleteDialog(player, category, context);
    }
}