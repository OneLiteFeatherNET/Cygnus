package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final Pos spawnPos;
    private final SetupItems setupItems;

    public PlayerSpawnListener(@NotNull Pos spawnPos, @NotNull SetupItems setupItems) {
        this.spawnPos = spawnPos;
        this.setupItems = setupItems;
    }

    @Override
    public void accept(@NotNull PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        player.teleport(spawnPos);
        player.setGameMode(GameMode.CREATIVE);
        setupItems.setMapSelection(player);
    }
}
