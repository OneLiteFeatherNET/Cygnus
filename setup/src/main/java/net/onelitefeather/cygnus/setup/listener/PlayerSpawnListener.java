package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final Pos spawnPos;
    private final SetupItems setupItems;
    private final Predicate<Instance> mainInstance;

    public PlayerSpawnListener(
            @NotNull Pos spawnPos,
            @NotNull SetupItems setupItems,
            @NotNull Predicate<Instance> mainInstance
    ) {
        this.spawnPos = spawnPos;
        this.setupItems = setupItems;
        this.mainInstance = mainInstance;
    }

    @Override
    public void accept(@NotNull PlayerSpawnEvent event) {
        Player player = event.getPlayer();

        if (!this.mainInstance.test(player.getInstance())) return;

        player.teleport(this.spawnPos);
        player.setGameMode(GameMode.CREATIVE);
        this.setupItems.setMapSelection(player);
    }
}
