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

public final class SpectatorChatListener implements Consumer<PlayerChatEvent> {

    private final Component prefix;
    private final Component separator;
    private final Predicate<Player> spectatorCheck;
    private final Supplier<Set<Player>> spectatorPlayer;

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

    private @NotNull Component formatMessage(@NotNull Player player, @NotNull String message) {
        Check.argCondition(player.getDisplayName() == null, "Player display name cannot be null");
        Component messageComponent = Component.text(message, NamedTextColor.GRAY);
        return prefix.append(Component.space()).append(player.getDisplayName()).append(Component.space()).append(separator).append(Component.space()).append(messageComponent);
    }
}
