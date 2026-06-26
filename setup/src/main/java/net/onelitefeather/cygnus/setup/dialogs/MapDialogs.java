package net.onelitefeather.cygnus.setup.dialogs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class MapDialogs extends DialogBase {

    public static final Key MAP_KEY = create("map_name");

    /**
     * Opens the dialog to allow the input of a name.
     *
     * @param player who should see the dialog
     */
    public static void openNameCreateDialog(Player player) {
        DialogTemplate dialogTemplate = DialogType.confirm(MAP_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Map setup"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Enter the name of the map")));
                    dialogMeta.text("name", textInputTemplate ->
                            textInputTemplate.maxLength(100).initial(""));
                })
                .yesButton(button -> button.width(101).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(MAP_KEY, getEmptyPayload()))
                )
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    public static void openNameUpdateDialog(Player player, String name) {
        DialogTemplate dialogTemplate = DialogType.confirm(MAP_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Map setup"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Update namer of the map")));
                    dialogMeta.text("name", textInputTemplate ->
                            textInputTemplate.maxLength(100).initial(name));
                })
                .yesButton(button -> button.width(101).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(MAP_KEY, getEmptyPayload()))
                )
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    public static void openDeleteDialog(Player player, MapDataCategory mapDataCategory) {

    }

    public static void openDeleteDialog(Player player, MapDataCategory mapDataCategory, @Nullable DialogContext context) {

    }

    private MapDialogs() {
    }
}
