package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.listener.SpectatorItemListener;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.onelitefeather.cygnus.utils.Items;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpectatorItemListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSpectateItemOpensOverviewForSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService spectatorService = new SpectatorService(spectatorTeam, survivorTeam);
        spectatorService.join(player);

        SpectatorItemListener listener = new SpectatorItemListener(spectatorService);
        ItemStack compass = ItemStack.builder(Material.COMPASS).set(Tags.ITEM_TAG, Items.SPECTATE_ITEM).build();

        listener.accept(new PlayerUseItemEvent(player, PlayerHand.MAIN, compass, 0L));

        assertNotNull(player.getOpenInventory());

        env.destroyInstance(instance, true);
    }

    @Test
    void testLeaveItemKicksSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService spectatorService = new SpectatorService(spectatorTeam, survivorTeam);
        spectatorService.join(player);

        SpectatorItemListener listener = new SpectatorItemListener(spectatorService);
        ItemStack door = ItemStack.builder(Material.OAK_DOOR).set(Tags.ITEM_TAG, Items.LEAVE_ITEM).build();

        listener.accept(new PlayerUseItemEvent(player, PlayerHand.MAIN, door, 0L));

        assertFalse(player.isOnline());

        env.destroyInstance(instance, true);
    }

    @Test
    void testNonSpectatorUsingSpectateItemIsIgnored(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService spectatorService = new SpectatorService(spectatorTeam, survivorTeam);

        SpectatorItemListener listener = new SpectatorItemListener(spectatorService);
        ItemStack compass = ItemStack.builder(Material.COMPASS).set(Tags.ITEM_TAG, Items.SPECTATE_ITEM).build();

        listener.accept(new PlayerUseItemEvent(player, PlayerHand.MAIN, compass, 0L));

        assertNull(player.getOpenInventory());

        env.destroyInstance(instance, true);
    }
}
