package net.onelitefeather.spectator;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.tag.Tag;
import net.onelitefeather.spectator.event.SpectatorAddEvent;
import net.onelitefeather.spectator.event.SpectatorRemoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DefaultSpectatorService implements SpectatorService {

    private static final Tag<Byte> SPECTATOR_TAG = Tag.Byte("spectator");
    private final Set<Player> spectators;

    public DefaultSpectatorService() {
        this.spectators = new HashSet<>();
    }

    @Override
    public void clear() {
        if (this.spectators.isEmpty()) return;
        this.spectators.clear();
    }

    @Override
    public boolean isSpectator(@NotNull Player player) {
        return spectators.contains(player) && player.getTag(SPECTATOR_TAG) == (byte) 1;
    }

    @Override
    public boolean hasSpectator() {
        return !this.spectators.isEmpty();
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        if (this.isSpectator(player)) return false;
        SpectatorAddEvent spectatorAddEvent = new SpectatorAddEvent(player);
        EventDispatcher.callCancellable(spectatorAddEvent, () -> {
            this.spectators.add(player);
            player.setTag(SPECTATOR_TAG, (byte) 1);
            EventDispatcher.call(new SpectatorAddEvent(player));
        });
        return true;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        if (!this.spectators.remove(player)) return false;
        player.removeTag(SPECTATOR_TAG);
        EventDispatcher.call(new SpectatorRemoveEvent(player));
        return true;
    }

    @Override
    public @NotNull Set<@NotNull Player> getViewers() {
        return Collections.unmodifiableSet(this.spectators);
    }
}
