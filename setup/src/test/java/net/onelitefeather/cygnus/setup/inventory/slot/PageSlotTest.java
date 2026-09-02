package net.onelitefeather.cygnus.setup.inventory.slot;

import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageSlotTest {

    private EventNode<Event> testNode;
    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        testNode = EventNode.all("test-node-" + UUID.randomUUID());
        env.process().eventHandler().addChild(testNode);
        instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(@NotNull Env env) {
        env.process().eventHandler().removeChild(testNode);
        env.destroyInstance(instance, true);
    }

    @Test
    void testGetItem() {
        PageResource resource = new PageResource(new Vec(10, 64, 20), Direction.NORTH);
        PageSlot slot = new PageSlot(resource);

        ItemStack item = slot.getItem();
        assertNotNull(item);
        assertTrue(item.has(DataComponents.LORE));
    }

    @Test
    void testLeftClickTeleportsInFrontOfPage(@NotNull Env env) {
        Player player = env.createPlayer(instance);
        Vec blockPos = new Vec(10, 64, 20);
        PageResource resource = new PageResource(blockPos, Direction.NORTH);
        PageSlot slot = new PageSlot(resource);

        Click.Left click = new Click.Left(0);
        slot.click(player, 0, click, ItemStack.AIR, _ -> {});

        Pos expectedPos = Helper.calculatePageTeleportPosition(blockPos, Direction.NORTH);
        assertEquals(expectedPos, player.getPosition());
    }

    @Test
    void testRightClickFiresPlayerRemoveDataEvent(@NotNull Env env) {
        Player player = env.createPlayer(instance);
        PageResource resource = new PageResource(new Vec(10, 64, 20), Direction.SOUTH);
        PageSlot slot = new PageSlot(resource);

        AtomicBoolean eventFired = new AtomicBoolean(false);
        testNode.addListener(PlayerRemoveDataEvent.class, event -> {
            assertEquals(player, event.getPlayer());
            eventFired.set(true);
        });

        Click.Right click = new Click.Right(0);
        slot.click(player, 0, click, ItemStack.AIR, _ -> {});

        assertTrue(eventFired.get(), "Right click must fire PlayerRemoveDataEvent");
    }

    @Test
    void testEqualsAndHashCode() {
        PageResource res1 = new PageResource(new Vec(10, 64, 20), Direction.NORTH);
        PageResource res2 = new PageResource(new Vec(10, 64, 20), Direction.NORTH);
        PageResource res3 = new PageResource(new Vec(10, 64, 20), Direction.SOUTH);

        PageSlot slot1 = new PageSlot(res1);
        PageSlot slot2 = new PageSlot(res2);
        PageSlot slot3 = new PageSlot(res3);

        assertEquals(slot1, slot2);
        assertEquals(slot1.hashCode(), slot2.hashCode());
        assertNotEquals(slot1, slot3);
    }
}
