package net.onelitefeather.cygnus.spectator;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorInventoryTest extends CygnusPlayerTestBase {

    @Test
    void testOpenShowsInventoryToPlayer(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, (spectator, target) -> {});
        inventory.open(player);

        assertEquals(inventory.getInventory(), player.getOpenInventory());

        env.destroyInstance(instance, true);
    }

    @Test
    void testHandleClickInvokesTeleportCallbackWithClickedTarget(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Player target = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(target);

        List<Player> teleportedTo = new ArrayList<>();
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, (s, t) -> teleportedTo.add(t));

        ItemStack headStack = ItemStack.builder(Material.PLAYER_HEAD)
                .set(SpectatorInventory.TARGET_TAG, target.getUuid())
                .build();

        inventory.handleClick(spectator, 10, null, headStack, _ -> {});

        assertEquals(1, teleportedTo.size());
        assertEquals(target, teleportedTo.getFirst());

        env.destroyInstance(instance, true);
    }

    @Test
    void testHandleClickIsNoOpWhenTargetIsOffline(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);

        List<Player> teleportedTo = new ArrayList<>();
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, (s, t) -> teleportedTo.add(t));

        ItemStack headStack = ItemStack.builder(Material.PLAYER_HEAD)
                .set(SpectatorInventory.TARGET_TAG, UUID.randomUUID())
                .build();

        inventory.handleClick(spectator, 10, null, headStack, _ -> {});

        assertTrue(teleportedTo.isEmpty());

        env.destroyInstance(instance, true);
    }
}
