package net.onelitefeather.cygnus.listener;

import net.theevilreaper.xerus.api.phase.Phase;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.resourcepack.ResourcePackService;
import net.onelitefeather.cygnus.spectator.LateJoinSlots;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PlayerLoginListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private final Supplier<Instance> instance;
    private final int maxPlayers;
    private final Supplier<Phase> currentPhase;
    private final Optional<ResourcePackService> resourcePackService;
    private final LateJoinSlots lateJoinSlots;

    /**
     * Creates the listener.
     *
     * @param instance            supplies the instance a player is sent into
     * @param maxPlayers          the lobby limit, which only applies while the lobby runs
     * @param currentPhase        reads the running phase
     * @param resourcePackService sends the resource pack, if one is configured
     * @param lateJoinSlots       the places for players who join a running round as spectators
     * @since 2.21.0
     */
    public PlayerLoginListener(Supplier<Instance> instance, int maxPlayers, Supplier<Phase> currentPhase,
                               Optional<ResourcePackService> resourcePackService, LateJoinSlots lateJoinSlots) {
        this.instance = instance;
        this.maxPlayers = maxPlayers;
        this.currentPhase = currentPhase;
        this.resourcePackService = resourcePackService;
        this.lateJoinSlots = lateJoinSlots;
    }

    /**
     * Admits the player or turns them away.
     *
     * <p>A player is turned away while still configuring, and a disconnect in that phase makes a proxy
     * move them to the fallback server inside the same configuration, which crashes the client with a
     * duplicate registry key. So the rejection is only recorded (see
     * {@link CygnusPlayer#kickInPlay(Component)}) and the configuration runs to its end; the kick is
     * delivered in the play state. The spawning instance is still set, because Minestom aborts the
     * configuration without one. A rejected player gets no ResourcePack, as they leave right away.</p>
     *
     * <p>In the lobby the player count is taken from the players in the play state, and a full server
     * turns the player away with {@link Messages#SERVER_FULL}. A player who is pending rejection is not
     * among them until the join packets are out and is removed again within the same step, so they do
     * not make other logins fail with a full server.</p>
     *
     * <p>While a round runs, the lobby limit does not apply. A joiner takes one of the late-join places
     * instead (see {@link LateJoinSlots}); the check and the reservation happen in one step. Without a
     * free place the joiner is turned away with {@link Messages#SPECTATOR_PLACES_FULL}, and when late
     * joining is off with {@link Messages#GAME_ALREADY_STARTED}. A joiner who gets a place is admitted
     * exactly like a lobby joiner and becomes a spectator at the first spawn.</p>
     *
     * @param event the configuration event of the joining player
     */
    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        if (currentPhase.get() instanceof LobbyPhase) {
            if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() + 1 > maxPlayers) {
                reject(player, Messages.SERVER_FULL, event);
                return;
            }
        } else if (!lateJoinSlots.enabled()) {
            reject(player, Messages.GAME_ALREADY_STARTED, event);
            return;
        } else if (!lateJoinSlots.tryReserve(player.getUuid())) {
            reject(player, Messages.SPECTATOR_PLACES_FULL, event);
            return;
        }
        event.setSpawningInstance(this.instance.get());
        resourcePackService.ifPresent(service -> service.sendTo(player));
    }

    private void reject(Player player, Component message, AsyncPlayerConfigurationEvent event) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            cygnusPlayer.kickInPlay(message);
            event.setSpawningInstance(this.instance.get());
        } else {
            player.kick(message);
        }
    }
}
