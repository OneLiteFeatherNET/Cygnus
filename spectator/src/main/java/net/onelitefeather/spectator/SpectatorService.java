package net.onelitefeather.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.onelitefeather.spectator.event.SpectatorRemoveEvent;
import net.onelitefeather.spectator.item.SpectatorItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Set;

/**
 * The SpectatorService interface defines the fundamental properties of a spectator service that manages
 * the spectator mode for players.
 * <p>
 * A SpectatorService represents a service that allows players to enter spectator mode, view the game world
 * from a different perspective, and interact with other spectators.
 * <p>
 * The SpectatorService interface is sealed and only permits extensions from the {@link SpectatorServiceBuilder} class.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SpectatorService {

    Tag<Byte> SPECTATOR_TAG = Tag.Byte("spectator");

    @Contract(pure = true)
    static @NotNull Builder builder() {
        return new SpectatorServiceBuilder();
    }

    /**
     * Removes all spectators from the service.
     * For each spectator, the {@link SpectatorRemoveEvent} is called.
     */
    void clear();

    /**
     * Adds a player to the spectator list.
     *
     * @param player the player to add
     * @return {@code true} if the player was added, {@code false} otherwise
     */
    boolean add(@NotNull Player player);

    /**
     * Removes a player from the spectator list.
     *
     * @param player the player to remove
     * @return {@code true} if the player was removed, {@code false} otherwise
     */
    boolean remove(@NotNull Player player);

    /**
     * Checks if a player is a spectator.
     *
     * @param player the player to check
     * @return {@code true} if the player is a spectator, {@code false} otherwise
     */
    boolean isSpectator(@NotNull Player player);

    /**
     * Checks if there are any spectators.
     *
     * @return {@code true} if there are spectators, {@code false} otherwise
     */
    boolean hasSpectators();

    /**
     * Gets all spectators.
     *
     * @return an unmodifiable view of all spectators
     */
    @NotNull
    @UnmodifiableView
    Set<Player> getSpectators();

    /**
     * A builder for creating a new {@link SpectatorService} instance.
     */
    sealed interface Builder permits SpectatorServiceBuilder {

        /**
         * Sets the prefix for the spectator list.
         *
         * @param prefix the prefix to set
         * @return this builder
         */
        @NotNull Builder prefix(@NotNull Component prefix);

        /**
         * Sets the separator for the spectator list.
         *
         * @param separator the separator to set
         * @return this builder
         */
        @NotNull Builder separator(@NotNull Component separator);

        /**
         * Sets the color for the spectator messages.
         * This effects only the message from the player itself and not the displayed display name from the player.
         *
         * @param messageColor the color to set
         * @return this builder
         */
        @NotNull Builder messageColor(@NotNull TextColor messageColor);

        /**
         * Adds a spectator item to the service.
         *
         * @param item the item to add
         * @return this builder
         */
        @NotNull Builder hotbarItem(@NotNull SpectatorItem item);

        /**
         * Detects when a spectator quits the server.
         *
         * @return this builder
         */
        @NotNull Builder detectSpectatorQuit();

        /**
         * Enables the spectator chat.
         *
         * @return this builder
         */
        @NotNull Builder spectatorChat();

        /**
         * Creates a new {@link SpectatorService} instance.
         *
         * @return the created instance
         */
        @Contract(pure = true)
        @NotNull SpectatorService build();
    }
}
