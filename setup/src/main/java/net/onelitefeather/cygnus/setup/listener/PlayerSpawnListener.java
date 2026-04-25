package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.setup.util.SetupItems;

import java.util.function.Consumer;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final Pos spawnPos;

    public PlayerSpawnListener(Pos spawnPos) {
        this.spawnPos = spawnPos;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();

        if (!event.isFirstSpawn()) return;

        player.teleport(this.spawnPos);
        player.setGameMode(GameMode.CREATIVE);
        SetupItems.setMapSelection(player);
    }
}
