package net.onelitefeather.cygnus.setup.listener.dialog;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.dialogs.MapDialogs;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;

import java.util.Map;
import java.util.function.Consumer;

public class DialogRequestListener implements Consumer<DialogRequestEvent> {

    private static final Map<DialogTarget, MapDataCategory> DELETE_TARGETS = Map.of(
            DialogTarget.DELETE_SPAWN, MapDataCategory.SPAWN,
            DialogTarget.DELETE_SURVIVOR_SPAWN, MapDataCategory.SURVIVOR,
            DialogTarget.DELETE_SLENDER, MapDataCategory.SLENDER,
            DialogTarget.DELETE_NAME, MapDataCategory.NAME
    );

    @Override
    public void accept(DialogRequestEvent event) {
        DialogTarget target = event.getTarget();
        Player player = event.getPlayer();
        DialogContext context = event.getContext();

        switch (target) {
            case CREATE_NAME -> MapDialogs.openNameCreateDialog(player);
            case UPDATE_NAME -> {
                if (context == null) return;
                MapDialogs.openNameUpdateDialog(player, ((DialogContext.NameContext) context).name());
            }
            case DELETE_PAGE_FACE -> {
                if (context == null) return;
                MapDialogs.openDeleteDialog(player, MapDataCategory.PAGE, context);
            }
            default -> {
                MapDataCategory category = DELETE_TARGETS.get(target);
                if (category == null || context == null) return;
                MapDialogs.openDeleteDialog(player, category);
            }
        }
    }
}
