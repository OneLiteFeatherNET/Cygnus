package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class PlayerPageInteractListener implements Consumer<PlayerEntityInteractEvent> {

    private final PageProvider pageProvider;

    public PlayerPageInteractListener(@NotNull PageProvider pageProvider) {
        this.pageProvider = pageProvider;
    }

    @Override
    public void accept(@NotNull PlayerEntityInteractEvent event) {
        if (event.getPlayer().getTag(Tags.TEAM_ID) != Helper.SURVIVOR_ID) return;
        if (!event.getTarget().hasTag(Tags.PAGE_TAG)) return;
        UUID uuid = event.getTarget().getTag(Tags.PAGE_TAG);
        pageProvider.triggerPageFound(event.getPlayer(), uuid);
    }
}
