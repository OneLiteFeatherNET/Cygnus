package net.onelitefeather.cygnus.setup.util;

import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.onelitefeather.cygnus.setup.util.FormatHelper.DECIMAL_FORMAT;

public final class LoreHelper {

    private LoreHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static @NotNull List<Component> getPosLore(@NotNull Point point) {
        List<Component> components = Components.pointToLore(MiniMessage.miniMessage(), point, DECIMAL_FORMAT);
        List<Component> loreList = new ArrayList<>();
        loreList.add(Component.empty());
        loreList.addAll(components);
        loreList.add(Component.empty());
        loreList.addAll(SetupMessages.ACTION_LORE);
        return loreList;
    }
}
