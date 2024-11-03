package net.onelitefeather.spectator.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.spectator.event.SpectatorQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SpectatorRemoveListener implements Consumer<PlayerDisconnectEvent> {

    private final Predicate<Player> spectatorCheck;
    private final Consumer<Player> removeSpectator;

    public SpectatorRemoveListener(@NotNull Predicate<Player> spectatorCheck, @NotNull Consumer<Player> removeSpectator) {
        this.spectatorCheck = spectatorCheck;
        this.removeSpectator = removeSpectator;
    }

    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        if (!this.spectatorCheck.test(player)) return;
        this.removeSpectator.accept(player);
        EventDispatcher.call(new SpectatorQuitEvent(player));
    }
}
