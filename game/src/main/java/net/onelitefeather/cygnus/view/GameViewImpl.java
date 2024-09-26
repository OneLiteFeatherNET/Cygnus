package net.onelitefeather.cygnus.view;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GameViewImpl implements GameView {

    private final BossBar bossBar;
    private final ViewUpdater updateFunction;

    public GameViewImpl(@NotNull ViewUpdater updateFunction) {
        this.updateFunction = updateFunction;
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    @Override
    public void updateViewers(boolean add) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (add) {
                onlinePlayer.showBossBar(this.bossBar);
            } else {
                onlinePlayer.hideBossBar(this.bossBar);
            }
        }
    }

    @Override
    public void updateView() {
        this.bossBar.name(this.updateFunction.updateView());
    }
}
