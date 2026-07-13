package net.onelitefeather.cygnus.common.strategy;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Functional interface to define which strategy should be used for the player teleportation.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.6
 */
@FunctionalInterface
public interface TeleportStrategy {

    /**
     * A strategy that shuffles the spawn points and distributes players evenly (round-robin) among them.
     */
    TeleportStrategy ROUND_ROBIN_RANDOM = (players, instance, spawns) -> {
        if (spawns.isEmpty()) {
            throw new IllegalStateException("The game map does not define any spawn points!");
        }
        var spawnList = new ArrayList<>(spawns);
        Collections.shuffle(spawnList);
        int index = 0;
        for (Player player : players) {
            Pos spawnPos = spawnList.get(index % spawnList.size());
            if (player.getInstance() != null && player.getInstance().equals(instance)) {
                player.teleport(spawnPos);
            } else {
                player.setInstance(instance, spawnPos);
            }
            index++;
        }
    };

    /**
     * A strategy that teleports all players to the first available spawn point.
     */
    TeleportStrategy SINGLE = (players, instance, spawns) -> {
        if (spawns.isEmpty()) {
            throw new IllegalStateException("The game map does not define any spawn points!");
        }
        Pos spawnPos = spawns.iterator().next();
        for (Player player : players) {
            if (player.getInstance() != null && player.getInstance().equals(instance)) {
                player.teleport(spawnPos);
            } else {
                player.setInstance(instance, spawnPos);
            }
        }
    };

    /**
     * Teleports a collection of players to a set of spawn positions within an instance.
     *
     * @param players  the players to teleport
     * @param instance the target instance
     * @param spawns   the available spawn locations
     */
    void teleport(Collection<Player> players, Instance instance, Collection<Pos> spawns);
}
