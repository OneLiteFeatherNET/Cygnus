package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.theevilreaper.aves.util.functional.PlayerConsumer;

import java.util.function.Consumer;

public class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final PlayerConsumer teleportConsumer;

    public PlayerSpawnListener(PlayerConsumer teleportConsumer) {
        this.teleportConsumer = teleportConsumer;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();

        if (!event.isFirstSpawn()) return;

        teleportConsumer.accept(player);
        player.setGameMode(GameMode.CREATIVE);
        SetupItems.setMapSelection(player);
    }
}
