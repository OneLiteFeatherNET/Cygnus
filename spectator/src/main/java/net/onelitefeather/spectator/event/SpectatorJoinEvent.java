package net.onelitefeather.spectator.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SpectatorJoinEvent implements PlayerEvent {

    private final Player player;

    SpectatorJoinEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }
}
