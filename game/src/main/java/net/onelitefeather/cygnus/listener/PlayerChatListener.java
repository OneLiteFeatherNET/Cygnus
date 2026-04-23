package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.player.PlayerChatEvent;

import java.util.function.Consumer;

public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);

    @Override
    public void accept(PlayerChatEvent event) {
        event.setFormattedMessage(this.setLobbyLayout(event));
    }

    private Component setLobbyLayout(PlayerChatEvent event) {
        return event.getPlayer().getDisplayName()
                .append(Component.space())
                .append(MESSAGE_PREFIX)
                .append(Component.space())
                .append(Component.text(event.getRawMessage(), NamedTextColor.GRAY)
                );
    }
}
