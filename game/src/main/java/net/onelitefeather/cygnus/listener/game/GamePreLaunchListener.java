package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.common.page.PageCalculation;
import net.onelitefeather.cygnus.common.util.HealthScalingCalculation;
import net.onelitefeather.cygnus.utils.TeamHelper;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

@SuppressWarnings("java:S3252")
public class GamePreLaunchListener implements Consumer<GamePreLaunchEvent> {

    private final ConnectionManager connectionManager;
    private final IntConsumer pageCounter;

    public GamePreLaunchListener(@NotNull IntConsumer pageCounter) {
        this.pageCounter = pageCounter;
        this.connectionManager = MinecraftServer.getConnectionManager();
    }

    @Override
    public void accept(@NotNull GamePreLaunchEvent event) {
        int pageCount = PageCalculation.calculatePageAmount();
        if (pageCount == 0) throw new UnsupportedOperationException("No pages found");
        pageCounter.accept(pageCount);

        float adjustedHealth = 0;

        if (pageCount <= GameConfig.MIN_PAGE_COUNT) {
            adjustedHealth = HealthScalingCalculation.getAdditionalHealth(pageCount);
        }

        for (Player player : connectionManager.getOnlinePlayers()) {
            AttributeHelper.adjustStepHeightAndJump(player);
            if (TeamHelper.isSlenderTeam(player)) continue;
            AttributeHelper.decreaseSpeed(player);
            if (adjustedHealth == 0) continue;
            AttributeHelper.updateHealthScale(player, adjustedHealth);
        }
    }
}
