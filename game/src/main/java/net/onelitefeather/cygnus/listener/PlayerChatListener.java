package net.onelitefeather.cygnus.listener;

import de.icevizion.xerus.api.phase.Phase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.player.PlayerChatEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);
    private final Phase currentPhase;


    public PlayerChatListener(@NotNull Phase currentPhase) {
        this.currentPhase = currentPhase;
    }

    @Override
    public void accept(@NotNull PlayerChatEvent playerChatEvent) {
        playerChatEvent.setChatFormat(this::setLobbyLayout);
        if (currentPhase instanceof GamePhase) {
            playerChatEvent.setChatFormat(this::setLobbyLayout);
        }
    }

    private @NotNull Component setLobbyLayout(@NotNull PlayerChatEvent event) {
        return event.getPlayer().getDisplayName().append(Component.space()).append(MESSAGE_PREFIX).append(Component.space()).append(Component.text(event.getMessage()));
    }
}
