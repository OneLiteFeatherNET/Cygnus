package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetTitleSubTitlePacket;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageGazeServiceTest {

    /** Ticks until a steady look shows the note: the third consecutive hit. */
    private static final int TICKS_TO_SHOW = 3;
    /** Consecutive misses that keep the note up: the sixth one takes it down. */
    private static final int MISSES_KEPT = 5;

    private static ItemStack pageItem(int modelId) {
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_" + modelId).asString())
                .build();
    }

    /**
     * Places a page so that its centre is at the player's eye height, {@code distance} blocks in
     * front of a player standing at the origin. A NORTH page sits at +0.5/+0.5/+1.0 of its block.
     */
    private static PageEntity placePage(Instance instance, Player player, double distance, ItemStack item) {
        Pos block = new Pos(-0.5, player.getEyeHeight() - 0.5, distance - 1.0);
        PageEntity page = PageFactory.createPage(new PageResource(block, Direction.NORTH), 1);
        page.place(instance).join();
        try {
            var field = PageEntity.class.getDeclaredField("pageItem");
            field.setAccessible(true);
            field.set(page, item);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return page;
    }

    /** Points the player at the page, turned away by {@code yawOffset} degrees. */
    private static void aim(Player player, PageEntity page, float yawOffset) {
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec toPage = Vec.fromPoint(page.getPosition().sub(eye));
        Pos look = Pos.ZERO.withDirection(toPage);
        player.teleport(player.getPosition().withView(look.yaw() + yawOffset, look.pitch())).join();
    }

    private static void ticks(PageGazeService service, int count) {
        for (int i = 0; i < count; i++) {
            service.tick();
        }
    }

    private static long textDisplays(Instance instance) {
        return instance.getEntities().stream().filter(e -> e.getEntityType() == EntityType.TEXT_DISPLAY).count();
    }

    @Test
    void steadyLookShowsNoteOnlyAfterDwell(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW - 1);
        assertFalse(service.isGazing(player), "a glance shorter than the dwell must not show the note");
        assertEquals(0, textDisplays(instance));

        service.tick();
        assertTrue(service.isGazing(player));
        assertEquals(page.getUuid(), service.activeGaze(player));
        assertEquals(1, textDisplays(instance));
        assertTrue(service.shownNote(player).entity().getViewers().contains(player));
    }

    @Test
    void interruptedDwellStartsOver(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW - 1);
        aim(player, page, 90);
        service.tick();
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW - 1);

        assertFalse(service.isGazing(player), "looking away must reset the dwell");
        service.tick();
        assertTrue(service.isGazing(player));
    }

    @Test
    void noTitleIsSentAnymore(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        Collector<SetTitleSubTitlePacket> subtitles = connection.trackIncoming(SetTitleSubTitlePacket.class);
        ticks(service, TICKS_TO_SHOW);

        assertTrue(service.isGazing(player));
        assertTrue(subtitles.collect().isEmpty());
    }

    @Test
    void wobblingInsideStayConeKeepsTheSameNote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(2));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);
        assertNotNull(shown);

        // cos(40°) ≈ 0.77: below the enter threshold, above the stay threshold
        for (int i = 0; i < 10; i++) {
            aim(player, page, i % 2 == 0 ? 40 : 20);
            service.tick();
            assertTrue(service.isGazing(player), "the note must not flicker at the cone edge");
        }
        assertEquals(shown, service.shownNote(player), "no new display may be spawned while wobbling");
        assertEquals(1, textDisplays(instance));
    }

    @Test
    void stayConeAloneDoesNotShowANote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(2));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 40);

        ticks(service, TICKS_TO_SHOW * 3);

        assertFalse(service.isGazing(player), "only the enter cone may start a note");
    }

    @Test
    void lookingAwayLingersThenHides(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(3));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        aim(player, page, 90);
        ticks(service, MISSES_KEPT);
        assertTrue(service.isGazing(player), "the note lingers for a moment after looking away");

        service.tick();
        assertFalse(service.isGazing(player));
        assertNull(service.activeGaze(player));
        env.tickWhile(() -> !entity.isRemoved(), Duration.ofSeconds(2));
        assertTrue(entity.isRemoved());
    }

    @Test
    void lookingBackDuringLingerKeepsTheNote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(3));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);

        aim(player, page, 90);
        ticks(service, MISSES_KEPT);
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 2);

        assertEquals(shown, service.shownNote(player));
    }

    @Test
    void walkingBeyondStayDistanceHidesAfterLinger(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);

        // 3 blocks back: about 5 blocks away, inside the stay distance
        player.teleport(player.getPosition().withZ(-3)).join();
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 2);
        assertTrue(service.isGazing(player), "5 blocks is still inside the stay distance");

        // 5 blocks back: about 7 blocks away
        player.teleport(player.getPosition().withZ(-5)).join();
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 1);
        assertFalse(service.isGazing(player));
    }

    @Test
    void pageBeyondEnterDistanceDoesNotShow(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 4.5, pageItem(3));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW * 2);

        assertFalse(service.isGazing(player));
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void nonInteractablePageRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        page.disableInteraction();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved(), "an expired page takes its note with it at once");
    }

    @Test
    void relocatedPageRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        // Moved a hair, so it is still in the cone: only the position check can catch it
        page.teleport(page.getPosition().add(0, 0, 0.01)).join();
        service.tick();

        assertTrue(entity.isRemoved(), "a moved page must not leave its note at the old spot");
        assertFalse(service.isGazing(player));
    }

    @Test
    void pageLeavingSupplierRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(5));
        aim(player, page, 0);
        List<PageEntity> pages = new ArrayList<>(List.of(page));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> pages);
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        pages.clear();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void pageWithoutNoteNeverShowsDisplay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, ItemStack.of(Material.PAPER));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW * 5);

        assertFalse(service.isGazing(player));
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void twoReadersGetSeparateDisplays(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance, Pos.ZERO);
        Player second = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, first, 2.0, pageItem(6));
        aim(first, page, 0);
        aim(second, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(first, second), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);

        PageNoteDisplay a = service.shownNote(first);
        PageNoteDisplay b = service.shownNote(second);
        assertNotSame(a, b);
        assertEquals(Set.of(first), a.entity().getViewers());
        assertEquals(Set.of(second), b.entity().getViewers());

        aim(first, page, 90);
        ticks(service, MISSES_KEPT + 1);
        assertFalse(service.isGazing(first));
        assertTrue(service.isGazing(second), "one reader looking away must not close the other's note");
    }

    @Test
    void switchingToAnotherPageReplacesTheNoteAfterDwell(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity left = placePage(instance, player, 2.0, pageItem(1));
        PageEntity right = PageFactory.createPage(
                new PageResource(new Pos(-3.5, player.getEyeHeight() - 0.5, 1.0), Direction.NORTH), 1);
        right.place(instance).join();
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(left, right));
        aim(player, left, 0);
        ticks(service, TICKS_TO_SHOW);
        Entity first = service.shownNote(player).entity();

        aim(player, right, 0);
        ticks(service, TICKS_TO_SHOW - 1);
        assertEquals(left.getUuid(), service.activeGaze(player), "the old note stays until the new dwell is done");

        service.tick();
        assertEquals(right.getUuid(), service.activeGaze(player));
        assertTrue(first.isRemoved(), "the old note goes at once when the new one appears");
    }

    @Test
    void survivorLeavingRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        List<Player> survivors = new ArrayList<>(List.of(player));
        PageGazeService service = new PageGazeService(() -> survivors, () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        survivors.clear();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void clearPlayerRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(5));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        service.clearPlayer(player);

        assertFalse(service.isGazing(player));
        assertNull(service.activeGaze(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void stopTaskRemovesAllNotes(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(6));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        service.startTask();
        assertTrue(service.isRunning());
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        service.stopTask();

        assertFalse(service.isRunning());
        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void stopTaskDuringFadeOutLeavesNoDisplay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(6));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);

        aim(player, page, 90);
        ticks(service, MISSES_KEPT + 1); // fade-out has started
        service.stopTask();
        for (int i = 0; i < PageNoteDisplay.HIDE_TICKS + 2; i++) {
            env.tick();
        }

        assertTrue(shown.entity().isRemoved());
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void constructorWithPageProvider(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);

        PageProvider pageProvider = new PageProvider();
        Set<PageResource> resources = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            resources.add(new PageResource(new Pos(i, player.getEyeHeight(), 2.0), Direction.NORTH));
        }
        pageProvider.loadPageData(resources);
        pageProvider.collectStartPages(resources.size());
        pageProvider.spawn(instance);

        PageGazeService service = new PageGazeService(() -> List.of(player), pageProvider);
        service.tick();

        assertFalse(pageProvider.interactablePages().isEmpty());
    }
}
