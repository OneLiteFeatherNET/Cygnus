package net.onelitefeather.cygnus.listener;

import net.theevilreaper.xerus.api.phase.Phase;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.phase.LobbyPhase;
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

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() + 1 > maxPlayers) {
            event.getPlayer().kick(Messages.SERVER_FULL);
            return;
        }
        if (!(currentPhase.get() instanceof LobbyPhase)) {
            event.getPlayer().kick(Messages.GAME_ALREADY_STARTED);
            return;
        }
        event.setSpawningInstance(this.instance.get());
        resourcePackService.ifPresent(service -> service.sendTo(event.getPlayer()));
    }
}
