package net.onelitefeather.spectator.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SpectatorRemoveListener implements Consumer<PlayerDisconnectEvent> {

    private final Predicate<Boolean> spectatorCheck;
    private final Consumer<Player> removeSpectator;

    public SpectatorRemoveListener(@NotNull Predicate<Boolean> spectatorCheck, @NotNull Consumer<Player> removeSpectator) {
        this.spectatorCheck = spectatorCheck;
        this.removeSpectator = removeSpectator;
    }

    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        if (!this.spectatorCheck.test(true)) return;
        Player player = event.getPlayer();
        this.removeSpectator.accept(player);
        EventDispatcher.call(new PlayerDisconnectEvent(player));
    }
}
