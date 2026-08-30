package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.NO_SPACE_SEPARATOR;

/**
 * The slot that shows a map's fog settings in the setup inventory.
 *
 * <p>The lore reports the two numbers a builder judges a map by - how far you can see and what
 * color the haze has - rather than all seven values, which would fill the tooltip without helping
 * anyone decide whether the map looks right.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmosphereSlot extends AbstractDataSlot {

    private final @Nullable MapAtmosphere atmosphere;

    /**
     * Creates a new slot for the given atmosphere.
     *
     * @param atmosphere the atmosphere of the map, or {@code null} if none is configured yet
     */
    public AtmosphereSlot(@Nullable MapAtmosphere atmosphere) {
        super(MapDataCategory.ATMOSPHERE);
        this.atmosphere = atmosphere;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getItem() {
        ItemStack overviewItem = MapDataCategory.getDefaultItem(type);

        if (atmosphere == null) {
            return overviewItem;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(describe("Sight", "%.0f blocks".formatted(atmosphere.fogEndDistance())));
        lore.add(describe("Fog", asHex(atmosphere)));
        lore.add(Component.empty());
        lore.add(DELETE_CLICK);
        lore.add(Component.empty());

        return asBuilder(overviewItem).lore(lore).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void click(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());

        switch (click) {
            case Click.Left _ -> EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.ATMOSPHERE_PRESET));
            case Click.Right _ when atmosphere != null ->
                    EventDispatcher.call(new PlayerRemoveDataEvent(player, type));
            default -> {
            }
        }
    }

    /**
     * Builds one labelled lore line in the category's color.
     *
     * @param label the name of the value
     * @param value the value itself
     * @return the rendered line
     */
    private Component describe(String label, String value) {
        return NO_SPACE_SEPARATOR
                .append(Component.space())
                .append(Component.text(label + ": ", NamedTextColor.WHITE))
                .append(Component.text(value, type.getColor()));
    }

    /**
     * Renders the fog color the same way it is written to {@code map.json}, so what a builder reads
     * here is what they will find in the file.
     *
     * @param atmosphere the atmosphere to read the fog color from
     * @return the fog color as {@code #RRGGBB}
     */
    private static String asHex(MapAtmosphere atmosphere) {
        return "#%02X%02X%02X".formatted(
                atmosphere.fogColor().red(),
                atmosphere.fogColor().green(),
                atmosphere.fogColor().blue()
        );
    }
}
