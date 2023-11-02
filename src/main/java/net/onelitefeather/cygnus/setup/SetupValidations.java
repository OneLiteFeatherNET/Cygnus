package net.onelitefeather.cygnus.setup;

import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SetupValidations {

    private static final Component MISSING_MAP = Component.text("Please select a map to setup and try the command again", NamedTextColor.RED);

    private SetupValidations() { }

    public static boolean argCondition(boolean condition, @NotNull CommandSender sender, @NotNull Component reason) {
        if (condition) {
            sender.sendMessage(reason);
            return true;
        }
        return false;
    }

    public static boolean mapCondition(@Nullable BaseMap baseMap, @NotNull CommandSender sender) {
        return argCondition(baseMap == null, sender, MISSING_MAP);
    }
}
