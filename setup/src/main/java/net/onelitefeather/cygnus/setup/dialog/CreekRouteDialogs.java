package net.onelitefeather.cygnus.setup.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;

/**
 * The dialog that asks for the name of a new creek route.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteDialogs extends DialogBase {

    public static final Key ROUTE_NAME_KEY = create("creek_route_name");

    /** Long enough for a descriptive name, short enough for the debug line. */
    private static final int MAX_NAME_LENGTH = 48;

    /**
     * Opens the dialog.
     *
     * @param player the player
     */
    public static void openNameDialog(Player player) {
        DialogTemplate dialogTemplate = DialogType.confirm(ROUTE_NAME_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Creek route"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Enter the name of the new route")));
                    dialogMeta.text("name", textInputTemplate ->
                            textInputTemplate.maxLength(MAX_NAME_LENGTH).initial(""));
                })
                .yesButton(button -> button.width(101).label(Component.text("Create"))
                        .action(new DialogAction.DynamicCustom(ROUTE_NAME_KEY, getEmptyPayload())))
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    private CreekRouteDialogs() {
    }
}
