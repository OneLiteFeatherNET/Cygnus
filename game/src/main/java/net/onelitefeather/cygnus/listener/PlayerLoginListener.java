package net.onelitefeather.cygnus.listener;

import de.icevizion.xerus.api.phase.Phase;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PlayerLoginListener implements Consumer<AsyncPlayerConfigurationEvent> {

    private static final Component KICK_COMPONENT = Messages.withMini("<red>The game has already started! Unable to join!");
    private final Instance instance;
    private final int maxPlayers;
    private final Supplier<Phase> currentPhase;

    public PlayerLoginListener(@NotNull Instance instance, int maxPlayers, Supplier<Phase> currentPhase) {
        this.instance = instance;
        this.maxPlayers = maxPlayers;
        this.currentPhase = currentPhase;
    }

    @Override
    public void accept(@NotNull AsyncPlayerConfigurationEvent event) {
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() + 1 > maxPlayers) {
            event.getPlayer().kick(KICK_COMPONENT);
            return;
        }
        if (!(currentPhase.get() instanceof LobbyPhase)) {
            event.getPlayer().kick(KICK_COMPONENT);
            return;
        }
        event.setSpawningInstance(this.instance);
    }
}
