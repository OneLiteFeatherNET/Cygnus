package net.onelitefeather.cygnus.setup.listener.dialog;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.dialog.AuthorDialogs;
import net.onelitefeather.cygnus.setup.dialog.MapDialogs;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;

import java.util.function.Consumer;

public class DialogRequestListener implements Consumer<DialogRequestEvent> {

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
            case CREATE_AUTHORS -> AuthorDialogs.openAuthorRequestDialog(player);
            case AUTHOR_INPUT -> {
                if (context == null) return;
                AuthorDialogs.openAuthorInput(player, ((DialogContext.AuthorAmount)context).amount());
            }
            default -> {
                // Nothing to do here
            }
        }
    }
}
