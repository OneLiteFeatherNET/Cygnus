package net.onelitefeather.cygnus.listener.page;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.page.PageGazeService;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public final class PlayerPageInteractListener implements Consumer<PlayerEntityInteractEvent> {

    private final PageProvider pageProvider;
    private final PageGazeService pageGazeService;

    public PlayerPageInteractListener(PageProvider pageProvider) {
        this(pageProvider, null);
    }

    public PlayerPageInteractListener(PageProvider pageProvider, @Nullable PageGazeService pageGazeService) {
        this.pageProvider = pageProvider;
        this.pageGazeService = pageGazeService;
    }

    @Override
    public void accept(PlayerEntityInteractEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getTarget();
        if (!TeamHelper.isSurvivorTeam(player) || !target.hasTag(Tags.PAGE_TAG)) return;
        UUID uuid = target.getTag(Tags.PAGE_TAG);
        if (this.pageProvider.triggerPageFound(player, uuid)) {
            if (player instanceof CygnusPlayer cygnusPlayer) {
                cygnusPlayer.incrementPageFound();
            }
            if (this.pageGazeService != null) {
                this.pageGazeService.clearPlayer(player);
            }
        }
    }
}
