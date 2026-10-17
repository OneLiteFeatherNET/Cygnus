package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsField;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForm;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.NO_SPACE_SEPARATOR;

/**
 * Shows the settings of a map in the overview and opens their dialog on a left click.
 *
 * @param <S> the settings record
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SettingsSlot<S> extends AbstractDataSlot {

    private final DialogTarget target;
    private final SettingsForm<S> form;
    private final S current;

    /**
     * Creates the slot.
     *
     * @param category the overview category
     * @param target   the dialog a click opens
     * @param form     describes the settings
     * @param current  the settings the map has right now
     */
    public SettingsSlot(MapDataCategory category, DialogTarget target, SettingsForm<S> form, S current) {
        super(category);
        this.target = target;
        this.form = form;
        this.current = current;
    }

    @Override
    public ItemStack getItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (SettingsField<S> field : this.form.fields()) {
            lore.add(describe(field.label(), format(field.getter().applyAsDouble(this.current), field.unit())));
        }
        lore.add(Component.empty());
        return asBuilder(MapDataCategory.getDefaultItem(type)).lore(lore).build();
    }

    @Override
    protected void click(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());
        if (click instanceof Click.Left) {
            EventDispatcher.call(new DialogRequestEvent(player, this.target,
                    new DialogContext.SettingsValues(this.form.values(this.current))));
        }
    }

    private Component describe(String label, String value) {
        return NO_SPACE_SEPARATOR
                .append(Component.space())
                .append(Component.text(label + ": ", NamedTextColor.WHITE))
                .append(Component.text(value, type.getColor()));
    }

    private static String format(double value, String unit) {
        String number = value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
        return unit.isEmpty() ? number : number + " " + unit;
    }
}
