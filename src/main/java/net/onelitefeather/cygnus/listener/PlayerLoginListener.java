package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.utils.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PlayerLoginListener implements Consumer<PlayerLoginEvent> {

    private static final Component KICK_COMPONENT = Messages.withMini("<red>The game has already started! Unable to join!");
    private final Instance instance;
    private final int maxPlayers;

    public PlayerLoginListener(@NotNull Instance instance, int maxPlayers) {
        this.instance = instance;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void accept(@NotNull PlayerLoginEvent event) {
        event.setSpawningInstance(this.instance);
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() + 1 > maxPlayers) {
            event.getPlayer().kick(KICK_COMPONENT);
            return;
        }
      //  event.getPlayer().updateViewableRule(ViewRuleUpdater::isViewAble);
      //  event.getPlayer().setAutoViewable(true);
    }
}
