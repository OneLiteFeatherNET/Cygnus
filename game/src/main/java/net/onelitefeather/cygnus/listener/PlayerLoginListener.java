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

    public PlayerLoginListener(Supplier<Instance> instance, int maxPlayers, Supplier<Phase> currentPhase, Optional<ResourcePackService> resourcePackService) {
        this.instance = instance;
        this.maxPlayers = maxPlayers;
        this.currentPhase = currentPhase;
        this.resourcePackService = resourcePackService;
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
     * <p>The player count is taken from the players in the play state. A player who is pending
     * rejection is not among them until the join packets are out and is removed again within the same
     * step, so they do not make other logins fail with a full server.</p>
     *
     * @param event the configuration event of the joining player
     */
    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() + 1 > maxPlayers) {
            reject(player, Messages.SERVER_FULL, event);
            return;
        }
        if (!(currentPhase.get() instanceof LobbyPhase)) {
            reject(player, Messages.GAME_ALREADY_STARTED, event);
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
