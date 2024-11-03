package net.onelitefeather.spectator.util;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the chat data for the spectator chat.
 *
 * @param prefix    the prefix for the chat message
 * @param separator the separator for the chat message
 * @since 1.0.0
 */
public record ChatData(@NotNull Component prefix, @NotNull Component separator) {
}
