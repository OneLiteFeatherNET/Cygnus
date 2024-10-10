package net.onelitefeather.cygnus.setup.listener;

import de.icevizion.aves.util.functional.PlayerConsumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PlayerDisconnectListener implements Consumer<PlayerDisconnectEvent> {

    private final PlayerConsumer dataRemover;

    public PlayerDisconnectListener(@NotNull PlayerConsumer dataRemover) {
        this.dataRemover = dataRemover;
    }

    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        dataRemover.accept(event.getPlayer());
        Component joinMessage = Messages.withPrefix(Component.text(player.getUsername(), NamedTextColor.AQUA))
                .append(Component.space())
                .append(Component.text("left the server", NamedTextColor.GRAY));
        Audience.audience(MinecraftServer.getConnectionManager().getOnlinePlayers())
                .sendMessage(joinMessage);
    }
}
