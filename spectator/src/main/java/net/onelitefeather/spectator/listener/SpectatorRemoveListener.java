package net.onelitefeather.spectator.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.spectator.event.SpectatorQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The SpectatorRemoveListener implements a {@link Consumer} that listens for {@link PlayerDisconnectEvent}s and removes the player from the spectator list if they are a spectator.
 * It also calls the {@link SpectatorQuitEvent} event.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SpectatorRemoveListener implements Consumer<PlayerDisconnectEvent> {

    private final Predicate<Player> spectatorCheck;
    private final Consumer<Player> removeSpectator;

    /**
     * Constructs a new SpectatorRemoveListener with the given {@link Predicate} and {@link Consumer}.
     *
     * @param spectatorCheck  the predicate to check if the player is a spectator
     * @param removeSpectator the consumer to remove the player from the spectator list
     */
    public SpectatorRemoveListener(@NotNull Predicate<Player> spectatorCheck, @NotNull Consumer<Player> removeSpectator) {
        this.spectatorCheck = spectatorCheck;
        this.removeSpectator = removeSpectator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        if (!this.spectatorCheck.test(player)) return;
        this.removeSpectator.accept(player);
        EventDispatcher.call(new SpectatorQuitEvent(player));
    }
}
