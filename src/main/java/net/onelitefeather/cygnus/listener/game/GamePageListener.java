package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.event.PageEvent;
import net.onelitefeather.cygnus.page.PageProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GamePageListener implements Consumer<PageEvent> {

    private final PageProvider pageProvider;

    public GamePageListener(@NotNull PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(@NotNull PageEvent event) {
        var pageEntity = event.entity();
        if (event.reason() == PageEvent.Reason.TTL) {
            this.pageProvider.triggerTTLHandling(pageEntity.getHitBoxUUID());
            return;
        }

        if (event.player() == null) {
            MinecraftServer.getExceptionManager().handleException(new IllegalStateException("A player can't be null when the reason is " + event.reason()));
            return;
        }

        this.pageProvider.triggerPageFound(event.player(), pageEntity.getHitBoxUUID());
    }
}
