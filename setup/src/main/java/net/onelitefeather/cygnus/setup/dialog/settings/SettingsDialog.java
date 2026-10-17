package net.onelitefeather.cygnus.setup.dialog.settings;

import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.util.DialogBase;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;

import java.util.Map;

/**
 * Opens the slider dialog of a {@link SettingsForm}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SettingsDialog extends DialogBase {

    private static final int BUTTON_WIDTH = 101;

    private SettingsDialog() {
        // Nothing to do here
    }

    /**
     * Opens the dialog, each slider on the given value.
     *
     * @param player  who should see the dialog
     * @param form    the settings to show
     * @param initial the values the sliders start at, keyed by field key
     */
    public static void open(Player player, SettingsForm<?> form, Map<String, Double> initial) {
        DialogTemplate template = DialogType.confirm(form.saveKey())
                .meta(meta -> {
                    meta.closeWithEscape(true);
                    meta.pause(false);
                    meta.afterAction(DialogAfterAction.CLOSE);
                    meta.title(Component.text(form.title()));
                    meta.emptyMessage();
                    meta.messageBody(body -> body.contents(
                            Component.text("These values only apply to this map.")));
                    for (SettingsField<?> field : form.fields()) {
                        meta.range(field.key(), range -> {
                            range.label(Component.text(field.label()));
                            range.width(BUTTON_WIDTH * 2);
                            range.start((float) field.min());
                            range.end((float) field.max());
                            range.step((float) field.step());
                            range.initial(initial.get(field.key()).floatValue());
                            range.labelFormat(field.unit().isEmpty() ? "%s: %s" : "%s: %s " + field.unit());
                        });
                    }
                })
                .yesButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Save"))
                        .action(new DialogAction.DynamicCustom(form.saveKey(), getEmptyPayload())))
                .noButton(button -> button.width(BUTTON_WIDTH).label(Component.text("Reset"))
                        .action(new DialogAction.DynamicCustom(form.resetKey(), getEmptyPayload())))
                .build();
        template.open(player);
    }
}
