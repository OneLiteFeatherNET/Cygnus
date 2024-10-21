package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.utils.TeamHelper;
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
        Player player = event.getPlayer();
        Entity target = event.getTarget();
        if (!TeamHelper.isSurvivorTeam(player) || !target.hasTag(Tags.PAGE_TAG)) return;
        UUID uuid = target.getTag(Tags.PAGE_TAG);
        pageProvider.triggerPageFound(player, uuid);
    }
}
