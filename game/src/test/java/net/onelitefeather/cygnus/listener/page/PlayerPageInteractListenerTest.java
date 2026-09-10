package net.onelitefeather.cygnus.listener.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.page.PageGazeService;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPageInteractListenerTest extends CygnusPlayerTestBase {

    @Test
    void testPagePickup(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        PageEntity pageEntity = PageFactory.createPage(instance, Pos.ZERO, Direction.NORTH, 1);
        UUID hitBoxUuid = pageEntity.getHitBoxUUID();
        seedActivePage(pageProvider, pageEntity);

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, hitBoxUuid);

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(1, player.getPageFounds(), "Counter must increment upon successfully finding a page");

        env.destroyInstance(instance, true);
    }

    @Test
    void testPagePickupClearsGaze(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        PageEntity pageEntity = PageFactory.createPage(instance, Pos.ZERO, Direction.NORTH, 1);
        UUID hitBoxUuid = pageEntity.getHitBoxUUID();
        seedActivePage(pageProvider, pageEntity);

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, hitBoxUuid);

        PageGazeService gazeService = new PageGazeService(List::of, List::of);
        Field activeGazeField = PageGazeService.class.getDeclaredField("activeGaze");
        activeGazeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Player, UUID> activeGaze = (Map<Player, UUID>) activeGazeField.get(gazeService);
        activeGaze.put(player, pageEntity.getUuid());

        assertTrue(gazeService.isGazing(player), "Player must initially be marked as gazing");

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider, gazeService);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(1, player.getPageFounds(), "Counter must increment upon successfully finding a page");
        assertFalse(gazeService.isGazing(player), "Active gaze must be cleared when player picks up the page");

        env.destroyInstance(instance, true);
    }

    @Test
    void testInvalidPageDoesNotClearGaze(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, UUID.randomUUID());

        PageGazeService gazeService = new PageGazeService(List::of, List::of);
        Field activeGazeField = PageGazeService.class.getDeclaredField("activeGaze");
        activeGazeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Player, UUID> activeGaze = (Map<Player, UUID>) activeGazeField.get(gazeService);
        UUID dummyId = UUID.randomUUID();
        activeGaze.put(player, dummyId);

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider, gazeService);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(0, player.getPageFounds());
        assertTrue(gazeService.isGazing(player), "Gaze state must remain when interaction was invalid");

        env.destroyInstance(instance, true);
    }

    @Test
    void testInvalidPage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, UUID.randomUUID());

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(0, player.getPageFounds(), "Counter must NOT increment if page was not found / already claimed");

        env.destroyInstance(instance, true);
    }

    @Test
    void testNonSurvivor(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        PageEntity pageEntity = PageFactory.createPage(instance, Pos.ZERO, Direction.NORTH, 1);
        UUID hitBoxUuid = pageEntity.getHitBoxUUID();
        seedActivePage(pageProvider, pageEntity);

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, hitBoxUuid);

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(0, player.getPageFounds());

        env.destroyInstance(instance, true);
    }

    @Test
    void testFourSidedBlock(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        Pos blockPos = new Pos(10, 64, 10);
        Direction[] directions = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

        Set<PageResource> resources = new HashSet<>();
        for (Direction direction : directions) {
            resources.add(new PageResource(blockPos, direction));
        }

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(resources);
        pageProvider.setMaxPageAmount(4);

        // Create 4 page entities at their wall-adjusted positions
        Map<Direction, PageEntity> entitiesByDir = new EnumMap<>(Direction.class);
        Map<Direction, Entity> targetsByDir = new EnumMap<>(Direction.class);

        int count = 1;
        for (Direction direction : directions) {
            Pos pagePos = Helper.updatePosition(blockPos, direction);
            PageEntity entity = PageFactory.createPage(instance, pagePos, direction, count++);
            entitiesByDir.put(direction, entity);
            seedActivePage(pageProvider, entity);

            Entity target = new Entity(EntityType.INTERACTION);
            target.setTag(Tags.PAGE_TAG, entity.getHitBoxUUID());
            targetsByDir.put(direction, target);
        }

        // Verify distinct positions and correct rotations
        Pos northPos = entitiesByDir.get(Direction.NORTH).getPosition();
        Pos southPos = entitiesByDir.get(Direction.SOUTH).getPosition();
        Pos eastPos = entitiesByDir.get(Direction.EAST).getPosition();
        Pos westPos = entitiesByDir.get(Direction.WEST).getPosition();

        assertEquals(0.0f, northPos.yaw());
        assertEquals(0.0f, southPos.yaw());
        assertEquals(-90.0f, eastPos.yaw());
        assertEquals(90.0f, westPos.yaw(), "West side must have yaw 90");

        assertEquals(4, Set.of(northPos, southPos, eastPos, westPos).size(), "All 4 page positions must be distinct");

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider);

        // Interact with each of the 4 pages one by one
        int expectedFound = 0;
        for (Direction direction : directions) {
            Entity target = targetsByDir.get(direction);
            listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));
            expectedFound++;
            assertEquals(expectedFound, player.getPageFounds(), "Counter must be " + expectedFound + " after collecting " + direction);
        }

        assertEquals(4, player.getPageFounds(), "Player must have collected all 4 pages");

        // Stale or invalid interact events must not increment the counter
        Entity staleTarget = new Entity(EntityType.INTERACTION);
        staleTarget.setTag(Tags.PAGE_TAG, UUID.randomUUID());
        listener.accept(new PlayerEntityInteractEvent(player, staleTarget, PlayerHand.MAIN, Vec.ZERO));
        assertEquals(4, player.getPageFounds(), "Counter must stay at 4 for unknown or stale page");

        // After game cleanUp, no further interaction increments counter
        pageProvider.cleanUp();
        for (Direction direction : directions) {
            Entity target = targetsByDir.get(direction);
            listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));
            assertEquals(4, player.getPageFounds(), "Counter must stay at 4 after cleanUp on " + direction);
        }

        env.destroyInstance(instance, true);
    }

    @SuppressWarnings("unchecked")
    private static void seedActivePage(PageProvider pageProvider, PageEntity entity) throws ReflectiveOperationException {
        Field field = PageProvider.class.getDeclaredField("activePages");
        field.setAccessible(true);
        Map<UUID, PageEntity> activePages = (Map<UUID, PageEntity>) field.get(pageProvider);
        activePages.put(entity.getHitBoxUUID(), entity);
    }
}
