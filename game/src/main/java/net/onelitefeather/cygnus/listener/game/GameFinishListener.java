package net.onelitefeather.cygnus.listener.game;

import de.icevizion.aves.util.Broadcaster;
import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GameFinishListener implements Consumer<GameFinishEvent> {

    @Override
    public void accept(@NotNull GameFinishEvent event) {
        var reason = event.reason();
        var player = event.player();

        Component endComponent = switch (reason) {
            case ALL_SURVIVOR_DEAD, SURVIVOR_LEFT -> Messages.getSlenderWinMessage(player);
            case TIME_OVER, ALL_PAGES_FOUND, SLENDER_LEFT -> Messages.SURVIVOR_WIN_MESSAGE;
        };
        Broadcaster.broadcast(endComponent);
    }
}
