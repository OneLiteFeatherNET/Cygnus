package net.onelitefeather.cygnus.setup.dialogs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;


/**
 *
 */
public final class MapDialogs extends DialogBase {

    public static final Key MAP_KEY = create("map_name");
    public static final Key AUTHOR_AMOUNT_KEY = create("author_amount_dialog");
    public static final Key AUTHOR_INPUT_ENTRY_KEY = create("author_input_dialog");
    public static final Key NON_DYNAMIC_DELETE_KEY = create("non_dynamic_delete_dialog");
    public static final Key DYNAMIC_DELETE_KEY = create("dynamic_delete_dialog");


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
                            template.contents(Component.text("Update name of the map")));
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
        DialogTemplate dialogTemplate = DialogType.confirm(NON_DYNAMIC_DELETE_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Map setup"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Do you want to delete the following map data?")));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template -> template.contents(Component.text("Category: ").append(Component.text(mapDataCategory.getName()))));
                })
                .yesButton(button -> button.width(101).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(NON_DYNAMIC_DELETE_KEY, getCategoryPayload(mapDataCategory.ordinal())))
                )
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    public static void openDeleteDialog(Player player, MapDataCategory mapDataCategory, @Nullable DialogContext context) {
        DialogContext.PositionContent positionContent = (DialogContext.PositionContent) context;
        DialogTemplate dialogTemplate = DialogType.confirm(DYNAMIC_DELETE_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Map setup"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Do you want to delete the following data?")));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template -> template.contents(Component.text("Category: ").append(Component.text(mapDataCategory.getName()))));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(componentTemplate -> {
                       componentTemplate.contents(Component.text("x: ", NamedTextColor.WHITE).append(Component.text(positionContent.point().x())));
                    });
                    dialogMeta.messageBody(componentTemplate -> {
                        componentTemplate.contents(Component.text("y: ", NamedTextColor.WHITE).append(Component.text(positionContent.point().y())));
                    });
                    dialogMeta.messageBody(componentTemplate -> {
                        componentTemplate.contents(Component.text("z: ", NamedTextColor.WHITE).append(Component.text(positionContent.point().z())));
                    });
                })
                .yesButton(button -> button.width(101).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(DYNAMIC_DELETE_KEY, getCategoryPayload(mapDataCategory.ordinal())))
                )
                .noButton(button -> button.width(101).label(NO_COMPONENT))
                .build();
        dialogTemplate.open(player);
    }

    private MapDialogs() {
    }

    /**
     * Returns a payload that contains the id of the frame to update.
     *
     * @param id the id of the frame
     * @return a payload
     */
    @Contract(pure = true)
    private static CompoundBinaryTag getCategoryPayload(int id) {
        return CompoundBinaryTag.builder().putInt("category_id", id).build();
    }
}
