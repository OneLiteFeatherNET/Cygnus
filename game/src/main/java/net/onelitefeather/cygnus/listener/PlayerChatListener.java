package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.player.PlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);

    @Override
    public void accept(@NotNull PlayerChatEvent event) {
        event.setFormattedMessage(this.setLobbyLayout(event));
    }

    private @NotNull Component setLobbyLayout(@NotNull PlayerChatEvent event) {
        return event.getPlayer().getDisplayName()
                .append(Component.space())
                .append(MESSAGE_PREFIX)
                .append(Component.space())
                .append(Component.text(event.getRawMessage(), NamedTextColor.GRAY)
                );
    }
}
