package net.onelitefeather.cygnus.view;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public final class GameViewImpl implements GameView {

    private final BossBar bossBar;
    private final ViewUpdater updateFunction;

    public GameViewImpl(@NotNull ViewUpdater updateFunction) {
        this.updateFunction = updateFunction;
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    }

    @Override
    public void updateView() {
        this.bossBar.name(this.updateFunction.updateView());
    }

    @Override
    public void addPlayer(@NotNull Player player, @Nullable Consumer<Player> consumer) {
        player.showBossBar(this.bossBar);
        if (consumer == null) return;
        consumer.accept(player);
    }

    @Override
    public void addPlayers(@NotNull Set<Player> set, @Nullable Consumer<Player> consumer) {
        set.forEach(player -> {
            player.showBossBar(this.bossBar);
            if (consumer == null) return;
            consumer.accept(player);
        });
    }

    @Override
    public void removePlayer(@NotNull Player player, @Nullable Consumer<Player> consumer) {
        player.hideBossBar(this.bossBar);
        if (consumer == null) return;
        consumer.accept(player);
    }

    @Override
    public void removePlayers(@NotNull Set<Player> set, @Nullable Consumer<Player> consumer) {
        set.forEach(player -> {
            player.hideBossBar(this.bossBar);
            if (consumer == null) return;
            consumer.accept(player);
        });
    }
}
