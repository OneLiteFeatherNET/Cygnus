package net.onelitefeather.cygnus.player.listener;

import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import net.onelitefeather.cygnus.spectator.SpectatorService;

import java.util.function.Consumer;

public final class SpectatorAddListener implements Consumer<SpectatorAddEvent> {

    private final SpectatorService spectatorService;

    public SpectatorAddListener(SpectatorService spectatorService) {
        this.spectatorService = spectatorService;
    }

    @Override
    public void accept(SpectatorAddEvent event) {
        spectatorService.join(event.getPlayer());
    }
}
