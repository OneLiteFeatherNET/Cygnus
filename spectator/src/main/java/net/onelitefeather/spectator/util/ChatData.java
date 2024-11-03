package net.onelitefeather.spectator.util;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the chat data for the spectator chat.
 *
 * @param prefix    the prefix for the chat message
 * @param separator the separator for the chat message
 * @since 1.0.0
 */
public record ChatData(@NotNull Component prefix, @NotNull Component separator) {

    /**
     * Creates a empty chat data instance.
     *
     * @return a new instance of the chat data
     */
    @Contract(pure = true)
    public static @NotNull ChatData empty() {
        return new ChatData(Component.empty(), Component.empty());
    }
}
