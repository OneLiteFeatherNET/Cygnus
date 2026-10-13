package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.config.EightPagesConfig;
import net.onelitefeather.cygnus.common.page.PageCalculation;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.util.SpeedScalingCalculation;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;

@SuppressWarnings("java:S3252")
public class GamePreLaunchListener implements Consumer<GamePreLaunchEvent> {

    private final ConnectionManager connectionManager;
    private final PageProvider pageProvider;
    private final EightPagesConfig eightPages;

    public GamePreLaunchListener(PageProvider pageProvider, EightPagesConfig eightPages) {
        this.pageProvider = pageProvider;
        this.eightPages = eightPages;
        this.connectionManager = MinecraftServer.getConnectionManager();
    }

    @Override
    public void accept(GamePreLaunchEvent event) {
        int survivors = Math.max(0, this.connectionManager.getOnlinePlayerCount() - 1);
        int pageCount = PageCalculation.calculatePageAmount(survivors);
        this.pageProvider.setMaxPageAmount(pageCount);
        // Only picked here, placed into the world once the round starts
        this.pageProvider.collectStartPages(PageCalculation.calculateActivePageAmount());

        // Based on the players, not on the page count: the page count carries a random jitter.
        // Drawn once, so all survivors of the round get the same whole hearts
        int bonus = this.eightPages.wholeHeartBonusFor(survivors);
        double adjustedSpeed = SpeedScalingCalculation.getAdditionalSpeed();

        for (Player player : connectionManager.getOnlinePlayers()) {
            AttributeHelper.adjustStepHeightAndJump(player);
            if (TeamHelper.isSlenderTeam(player)) continue;
            AttributeHelper.decreaseSpeed(player);
            if (player instanceof CygnusPlayer cygnus) {
                cygnus.vitality().setUp(bonus);
            }
            if (adjustedSpeed != 0) {
                AttributeHelper.updateSpeedScale(player, adjustedSpeed);
            }
        }
    }
}
