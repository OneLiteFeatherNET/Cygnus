package net.onelitefeather.cygnus.setup.util;

import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains some validations checks which are required for the setup.
 * @version 1.0.0
 * @since 1.0.0
 * @author theEvilReaper
 */
public final class SetupValidations {

    private static final Component MISSING_MAP = Component.text("Please select a map to setup and try the command again", NamedTextColor.RED);

    private SetupValidations() { }

    /**
     * Checks if a given condition is reached or not.
     * If the condition is reached it will send a message
     * @param condition the condition to send
     * @param sender the involved {@link CommandSender}
     * @param reason the message to send
     * @return true when the condition is reached otherwise false
     */
    public static boolean argCondition(boolean condition, @NotNull CommandSender sender, @NotNull Component reason) {
        if (condition) {
            sender.sendMessage(reason);
            return true;
        }
        return false;
    }

    /**
     * Returns a boolean indicator if the provided {@link BaseMap} reference is null or not.
     * If the reference is null it sends a predefined method to the player
     * @param baseMap the map to check
     * @param sender the involved {@link CommandSender}
     * @return true when the map is not null otherwise false
     */
    public static boolean mapCondition(@Nullable BaseMap baseMap, @NotNull CommandSender sender) {
        return argCondition(baseMap == null, sender, MISSING_MAP);
    }
}
