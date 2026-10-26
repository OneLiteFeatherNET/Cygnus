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
 * The dialog that asks for the name of a new page pool.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PagePoolDialogs extends DialogBase {

    public static final Key POOL_NAME_KEY = create("page_pool_name");

    /** The name is the file name of the pool, so it stays short. */
    private static final int MAX_NAME_LENGTH = 32;

    /**
     * Opens the dialog.
     *
     * @param player the player
     */
    public static void openNameDialog(Player player) {
        DialogTemplate dialogTemplate = DialogType.confirm(POOL_NAME_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Page pool"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Enter the name of the new pool")));
                    dialogMeta.text("name", textInputTemplate ->
                            textInputTemplate.maxLength(MAX_NAME_LENGTH).initial(""));
                })
                .yesButton(button -> button.width(101).label(Component.text("Create"))
                        .action(new DialogAction.DynamicCustom(POOL_NAME_KEY, getEmptyPayload())))
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    private PagePoolDialogs() {
        // Nothing to do here
    }
}
