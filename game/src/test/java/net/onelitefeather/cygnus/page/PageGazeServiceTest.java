package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ClearTitlesPacket;
import net.minestom.server.network.packet.server.play.SetTitleSubTitlePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.ui.TooltipBox;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageGazeServiceTest {

    private ItemStack createPageItem(int modelId) {
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_" + modelId).asString())
                .build();
    }

    private PageEntity createPageEntity(Instance instance, Pos pos, ItemStack item) {
        PageEntity entity = PageFactory.createPage(instance, pos, Direction.NORTH, 1);
        try {
            var field = PageEntity.class.getDeclaredField("pageItem");
            field.setAccessible(true);
            field.set(entity, item);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    @Test
    void testLookingAtPageTriggersTooltip(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0); // yaw=0 -> facing +Z
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(1);
        Pos pagePos = new Pos(0, player.getEyeHeight(), 2.0); // directly in front at distance 2
        PageEntity page = createPageEntity(instance, pagePos, pageItem);

        List<PageEntity> pages = List.of(page);
        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> pages);

        Collector<SetTitleSubTitlePacket> subtitleCollector = connection.trackIncoming(SetTitleSubTitlePacket.class);
        gazeService.tick();

        assertTrue(gazeService.isGazing(player));
        assertEquals(page.getUuid(), gazeService.activeGaze(player));

        var packets = subtitleCollector.collect();
        assertEquals(1, packets.size());

        Component expectedSubtitle = TooltipBox.of(PageNote.forItem(pageItem).orElseThrow());
        assertEquals(expectedSubtitle, packets.get(0).subtitle());
    }

    @Test
    void testLookingAwayClearsTitle(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0); // facing +Z
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(2);
        Pos pagePos = new Pos(0, player.getEyeHeight(), 2.0);
        PageEntity page = createPageEntity(instance, pagePos, pageItem);

        List<PageEntity> pages = new ArrayList<>();
        pages.add(page);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> pages);

        gazeService.tick();
        assertTrue(gazeService.isGazing(player));

        // Looking away: remove page or page moves out of gaze
        pages.clear();

        Collector<ClearTitlesPacket> clearCollector = connection.trackIncoming(ClearTitlesPacket.class);
        gazeService.tick();

        assertFalse(gazeService.isGazing(player));
        assertNull(gazeService.activeGaze(player));

        var packets = clearCollector.collect();
        assertEquals(1, packets.size());
        assertFalse(packets.get(0).reset());
    }

    @Test
    void testPageBeyondMaxDistanceDoesNotTrigger(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0); // facing +Z
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(3);
        Pos pagePos = new Pos(0, player.getEyeHeight(), 4.5); // 4.5 blocks > MAX_GAZE_DISTANCE (4.0)
        PageEntity page = createPageEntity(instance, pagePos, pageItem);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(page));
        Collector<SetTitleSubTitlePacket> subtitleCollector = connection.trackIncoming(SetTitleSubTitlePacket.class);

        gazeService.tick();

        assertFalse(gazeService.isGazing(player));
        assertNull(gazeService.activeGaze(player));
        assertTrue(subtitleCollector.collect().isEmpty());
    }

    @Test
    void testNonInteractablePageDoesNotTrigger(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0);
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(4);
        PageEntity page = createPageEntity(instance, new Pos(0, player.getEyeHeight(), 2.0), pageItem);
        page.disableInteraction();

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(page));
        gazeService.tick();

        assertFalse(gazeService.isGazing(player));
        assertNull(gazeService.activeGaze(player));
    }

    @Test
    void testPageWithoutNoteDoesNotTriggerTitle(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0);
        player.teleport(playerPos);

        ItemStack regularItem = ItemStack.of(Material.PAPER); // No PageNote tag
        Pos pagePos = new Pos(0, player.getEyeHeight(), 2.0);
        PageEntity page = createPageEntity(instance, pagePos, regularItem);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(page));
        Collector<SetTitleSubTitlePacket> subtitleCollector = connection.trackIncoming(SetTitleSubTitlePacket.class);

        gazeService.tick();

        assertFalse(gazeService.isGazing(player));
        assertTrue(subtitleCollector.collect().isEmpty());
    }

    @Test
    void testClearPlayerClearsTitleAndState(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0);
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(5);
        Pos pagePos = new Pos(0, player.getEyeHeight(), 2.0);
        PageEntity page = createPageEntity(instance, pagePos, pageItem);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(page));
        gazeService.tick();
        assertTrue(gazeService.isGazing(player));

        Collector<ClearTitlesPacket> clearCollector = connection.trackIncoming(ClearTitlesPacket.class);
        gazeService.clearPlayer(player);

        assertFalse(gazeService.isGazing(player));
        assertNull(gazeService.activeGaze(player));

        var packets = clearCollector.collect();
        assertEquals(1, packets.size());
    }

    @Test
    void testStopTaskClearsAllTitles(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        Pos playerPos = new Pos(0, 0, 0, 0, 0);
        player.teleport(playerPos);

        ItemStack pageItem = createPageItem(6);
        Pos pagePos = new Pos(0, player.getEyeHeight(), 2.0);
        PageEntity page = createPageEntity(instance, pagePos, pageItem);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(page));
        gazeService.startTask();
        assertTrue(gazeService.isRunning());

        gazeService.tick();
        assertTrue(gazeService.isGazing(player));

        Collector<ClearTitlesPacket> clearCollector = connection.trackIncoming(ClearTitlesPacket.class);
        gazeService.stopTask();

        assertFalse(gazeService.isRunning());
        assertFalse(gazeService.isGazing(player));
        assertEquals(1, clearCollector.collect().size());
    }

    @Test
    void testConstructorWithPageProvider(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);

        PageProvider pageProvider = new PageProvider();
        Set<PageResource> resources = new java.util.HashSet<>();
        for (int i = 0; i < 10; i++) {
            resources.add(new PageResource(new Pos(i, player.getEyeHeight(), 2.0), Direction.NORTH));
        }
        pageProvider.loadPageData(resources);
        pageProvider.collectStartPages(instance);

        PageGazeService gazeService = new PageGazeService(() -> List.of(player), pageProvider);
        gazeService.tick();

        assertFalse(pageProvider.interactablePages().isEmpty());
    }
}
