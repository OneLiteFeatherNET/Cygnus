package net.onelitefeather.cygnus.player;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryLockTest extends CygnusPlayerTestBase {

    private static final ItemStack ITEM = ItemStack.of(Material.ENDER_EYE);

    private static InventoryLock lock(Env env) {
        InventoryLock lock = new InventoryLock();
        env.process().eventHandler().addChild(lock.node());
        return lock;
    }

    @Test
    @DisplayName("Dropping, moving and swapping into the offhand are cancelled")
    void locked(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 40, 0));
        player.setGameMode(GameMode.ADVENTURE);
        InventoryLock lock = lock(env);

        ItemDropEvent drop = new ItemDropEvent(player, ITEM);
        PlayerSwapItemEvent swap = new PlayerSwapItemEvent(player, ITEM, ItemStack.AIR);
        InventoryPreClickEvent click = new InventoryPreClickEvent(player.getInventory(), player, new Click.Left(0));
        EventDispatcher.call(drop);
        EventDispatcher.call(swap);
        EventDispatcher.call(click);

        assertTrue(drop.isCancelled());
        assertTrue(swap.isCancelled());
        assertTrue(click.isCancelled());
        env.process().eventHandler().removeChild(lock.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Clicks in an opened inventory are left to that inventory")
    void openedInventoryIsFree(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 40, 0));
        InventoryLock lock = lock(env);

        InventoryPreClickEvent click = new InventoryPreClickEvent(
                new Inventory(InventoryType.CHEST_1_ROW, Component.text("Menu")), player, new Click.Left(0));
        EventDispatcher.call(click);

        assertFalse(click.isCancelled());
        env.process().eventHandler().removeChild(lock.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Creative players stay free")
    void creativeIsFree(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(0, 40, 0));
        player.setGameMode(GameMode.CREATIVE);
        InventoryLock lock = lock(env);

        ItemDropEvent drop = new ItemDropEvent(player, ITEM);
        EventDispatcher.call(drop);

        assertFalse(drop.isCancelled());
        env.process().eventHandler().removeChild(lock.node());
        env.destroyInstance(instance, true);
    }
}
