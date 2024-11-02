package net.onelitefeather.spectator.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SpectatorRemoveEvent implements PlayerEvent {

    private final Player player;

    public SpectatorRemoveEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }
}
