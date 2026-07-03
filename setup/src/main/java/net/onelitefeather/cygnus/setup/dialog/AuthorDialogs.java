package net.onelitefeather.cygnus.setup.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;

public final class AuthorDialogs extends DialogBase {

    public static final Key AUTHOR_AMOUNT_KEY = create("author_amount_dialog");
    public static final Key AUTHOR_INPUT_ENTRY_KEY = create("author_input_dialog");

    public static void openAuthorRequestDialog(Player player) {
        DialogTemplate dialogTemplate = DialogType.confirm(AUTHOR_AMOUNT_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Author setup"));
                    dialogMeta.emptyMessage();
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("How many builders should the map have?")));
                    dialogMeta.range("amount", range -> range
                            .label(Component.text("Amount"))
                            .start(1)
                            .initial(1)
                            .end(10)
                            .step(1));
                })
                .yesButton(button -> button.width(100).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(AUTHOR_AMOUNT_KEY, CompoundBinaryTag.builder().build()))
                )
                .noButton(button -> button.width(101).label(Component.text("Cancel")))
                .build();
        dialogTemplate.open(player);
    }

    public static void openAuthorInput(Player player, float submitFields) {
        DialogTemplate dialogTemplate = DialogType.confirm(AUTHOR_INPUT_ENTRY_KEY)
                .meta(dialogMeta -> {
                    dialogMeta.closeWithEscape(false);
                    dialogMeta.pause(false);
                    dialogMeta.afterAction(DialogAfterAction.CLOSE);
                    dialogMeta.title(Component.text("Author setup"));
                    dialogMeta.messageBody(template ->
                            template.contents(Component.text("Please enter the builder(s)")));

                    for (int i = 0; i < submitFields; i++) {
                        dialogMeta.text("author_" + i, textInputTemplate ->
                                textInputTemplate.activeLabel(true).label(Component.text("Author: ")).maxLength(32).initial(""));
                    }
                })
                .yesButton(button -> button.width(100).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(AUTHOR_INPUT_ENTRY_KEY, CompoundBinaryTag.builder()
                                .putFloat("amount", submitFields)
                                .build()
                        ))
                )
                .noButton(button -> button.width(101).label(Component.text("Cancel")))
                .build();
        dialogTemplate.open(player);
    }

    private AuthorDialogs() {
        // Nothing to do here
    }
}
