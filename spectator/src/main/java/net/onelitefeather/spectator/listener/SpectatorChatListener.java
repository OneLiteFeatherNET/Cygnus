package net.onelitefeather.spectator.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The {@code SpectatorChatListener} class is a listener that listens for chat messages from spectators.
 * When a spectator sends a message, the message is sent to all other spectators.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SpectatorChatListener implements Consumer<PlayerChatEvent> {

    private final Component prefix;
    private final Component separator;
    private final Predicate<Player> spectatorCheck;
    private final Supplier<Set<Player>> spectatorPlayer;

    /**
     * Creates a new spectator chat listener
     *
     * @param prefix          the prefix to be used in the message
     * @param separator       the separator to be used in the message
     * @param spectatorCheck  the predicate to check if a player is a spectator
     * @param spectatorPlayer the supplier to get the set of spectators
     */
    public SpectatorChatListener(
            @NotNull Component prefix,
            @NotNull Component separator,
            @NotNull Predicate<Player> spectatorCheck,
            @NotNull Supplier<Set<Player>> spectatorPlayer
    ) {
        this.prefix = prefix;
        this.separator = separator;
        this.spectatorCheck = spectatorCheck;
        this.spectatorPlayer = spectatorPlayer;
    }

    @Override
    public void accept(@NotNull PlayerChatEvent event) {
        if (!spectatorCheck.test(event.getPlayer())) return;
        event.setCancelled(true);
        Set<Player> players = spectatorPlayer.get();
        for (Player player : players) {
            player.sendMessage(formatMessage(event.getPlayer(), event.getMessage()));
        }
    }

    /**
     * Formats the message to be sent to spectators
     *
     * @param player  the player who sent the message
     * @param message the message to be sent
     * @return the formatted message
     */
    private @NotNull Component formatMessage(@NotNull Player player, @NotNull String message) {
        Check.argCondition(player.getDisplayName() == null, "Player display name cannot be null");
        Component messageComponent = Component.text(message, NamedTextColor.GRAY);
        return prefix.append(Component.space()).append(player.getDisplayName()).append(Component.space()).append(separator).append(Component.space()).append(messageComponent);
    }
}
