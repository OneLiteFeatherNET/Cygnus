package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.onelitefeather.cygnus.common.config.GameConfig.MIN_ACTIVE_PAGE_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageProviderTest {

    @Test
    void testLoadingPagesTwiceIsRejected() {
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(spots(1));

        Set<PageResource> values = spots(1);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageProvider.loadPageData(values)
        );
        assertEquals("Can't load pages twice", exception.getMessage());
    }

    @Test
    void testLoadingNoPagesIsRejected() {
        PageProvider pageProvider = new PageProvider();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pageProvider.loadPageData(Set.of())
        );
        assertEquals("Can't load a map without any pages", exception.getMessage());
    }

    @Test
    void testCollectStartPagesUsesTheGivenActivePageCount() {
        int activePageCount = 12;
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(spots(activePageCount));

        pageProvider.collectStartPages(activePageCount);

        assertEquals(activePageCount, pageProvider.interactablePages().size(),
                "collectStartPages must collect exactly the requested active page count");
    }

    @Test
    void testCollectStartPagesRejectsAnActivePageCountAboveTheAvailableData() {
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(spots(8));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageProvider.collectStartPages(12)
        );
        assertEquals("Not enough pages to start the game", exception.getMessage());
    }

    @Test
    void testCollectedPagesOnlyAppearOnSpawn(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(spots(MIN_ACTIVE_PAGE_COUNT));

        pageProvider.collectStartPages(MIN_ACTIVE_PAGE_COUNT);
        assertTrue(pageProvider.interactablePages().stream().allMatch(page -> page.getInstance() == null),
                "collecting must not put any page into the world yet");

        pageProvider.spawn(instance);
        assertTrue(pageProvider.interactablePages().stream().allMatch(page -> page.getInstance() == instance),
                "spawn must place every collected page");

        env.destroyInstance(instance, true);
    }

    @Test
    void testInteractablePagesOnlyListsCollectiblePages(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);

        PageEntity expired = pageProvider.interactablePages().getFirst();
        expired.disableInteraction();

        assertEquals(MIN_ACTIVE_PAGE_COUNT - 1, pageProvider.interactablePages().size());
        assertFalse(pageProvider.interactablePages().contains(expired),
                "an expired page is invisible to the player and must not be announced by a sound");

        env.destroyInstance(instance, true);
    }

    @Test
    void testAClaimOnAnUnknownPageCountsNothing(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        Player player = env.createPlayer(instance);
        AtomicInteger events = new AtomicInteger();
        env.process().eventHandler().addListener(PageFoundEvent.class, event -> events.incrementAndGet());

        assertFalse(pageProvider.triggerPageFound(player, UUID.randomUUID()), "a claim on a missing uuid must return false");
        assertEquals(0, events.get(), "a claim that finds nothing must not raise the tension");

        env.destroyInstance(instance, true);
    }

    @Test
    void testEveryFindFiresAnEventCarryingTheRunningCount(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        int pageCount = 3;
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        pageProvider.setMaxPageAmount(pageCount);
        Player player = env.createPlayer(instance);

        List<PageFoundEvent> events = Collections.synchronizedList(new ArrayList<>());
        env.process().eventHandler().addListener(PageFoundEvent.class, events::add);

        for (PageEntity page : pageProvider.interactablePages().subList(0, pageCount)) {
            pageProvider.triggerPageFound(player, page.getHitBoxUUID());
        }

        assertEquals(List.of(1, 2, 3), events.stream().map(PageFoundEvent::foundCount).toList(),
                "each find has to report how many pages are gone by now, not just that one was found");
        assertEquals(pageCount, events.getFirst().maxPages());
        assertSame(player, events.getFirst().finder());

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the race where two players interact with the same page hitbox at almost the same time.
     * Before the fix, the losing call dereferenced a {@code null} {@link PageEntity} and threw an NPE;
     * it must now bail out silently and the winner's count must still be recorded correctly.
     */
    @Test
    void testConcurrentDuplicateFind(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        pageProvider.setMaxPageAmount(1);
        Player player = env.createPlayer(instance);
        UUID uuid = pageProvider.interactablePages().getFirst().getHitBoxUUID();

        AtomicInteger completedEvents = new AtomicInteger();
        env.process().eventHandler().addListener(PageDiscoveryCompletedEvent.class, event -> completedEvents.incrementAndGet());

        runConcurrently(Collections.nCopies(8, (Runnable) () -> pageProvider.triggerPageFound(player, uuid)));

        assertEquals(1, completedEvents.get(), "the completion event must fire exactly once, not zero or more than once");
        assertEquals("1 / 1", plainStatus(pageProvider));

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the lost-update race on the found counter: with a plain {@code int} and {@code ++},
     * concurrent finds of distinct pages could overwrite each other's increment and the displayed
     * count would end up below the real number found, sometimes preventing the completion event from
     * ever firing.
     */
    @Test
    void testConcurrentDistinctFinds(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        pageProvider.setMaxPageAmount(MIN_ACTIVE_PAGE_COUNT);
        Player player = env.createPlayer(instance);

        AtomicInteger completedEvents = new AtomicInteger();
        env.process().eventHandler().addListener(PageDiscoveryCompletedEvent.class, event -> completedEvents.incrementAndGet());

        runConcurrently(findTasks(pageProvider, player));

        assertEquals(MIN_ACTIVE_PAGE_COUNT + " / " + MIN_ACTIVE_PAGE_COUNT, plainStatus(pageProvider),
                "every concurrent find must be counted, a lost update would leave the status below " + MIN_ACTIVE_PAGE_COUNT);
        assertEquals(1, completedEvents.get(), "the completion event must fire exactly once once all pages are found");

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the race between game-end cleanup and an in-flight pickup: before the fix,
     * {@code cleanUp()} iterated the map without any guard while another thread could mutate it
     * concurrently via {@code triggerPageFound}.
     */
    @Test
    void testConcurrentCleanUp(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        Player player = env.createPlayer(instance);

        List<Runnable> tasks = new ArrayList<>(findTasks(pageProvider, player));
        tasks.add(pageProvider::cleanUp);

        assertDoesNotThrow(() -> runConcurrently(tasks));

        env.destroyInstance(instance, true);
    }

    @Test
    void testAnExpiredPageMovesOnAndHandsItsSpotBack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        // One spare spot: without the expired spots coming back, the second expiry finds the pool empty
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT + 1);
        AtomicInteger finds = new AtomicInteger();
        env.process().eventHandler().addListener(PageFoundEvent.class, event -> finds.incrementAndGet());
        String statusBefore = plainStatus(pageProvider);

        PageEntity page = pageProvider.interactablePages().getFirst();
        Pos startSpot = page.getPosition();

        pageProvider.triggerTTLHandling(page.getHitBoxUUID());
        assertNotEquals(startSpot, page.getPosition(), "an expired page must move to a new spot");
        assertTrue(page.isInteractable(), "the expired page is collectible again on its new spot");
        assertEquals(0, finds.get(), "an expiry must not raise a find");
        assertEquals(statusBefore, plainStatus(pageProvider), "an expiry must not change the found count");

        pageProvider.triggerTTLHandling(page.getHitBoxUUID());
        assertEquals(startSpot, page.getPosition(), "the spot it expired on must be back in the pool");

        env.destroyInstance(instance, true);
    }

    @Test
    void testAFoundPageStartsFreshOnTheSpareSpotAndUsesItsSpotUp(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT + 1);
        Player player = env.createPlayer(instance);

        PageEntity page = pageProvider.interactablePages().getFirst();
        Pos foundAt = page.getPosition();
        // Almost run out on its old spot
        Field tickTime = PageEntity.class.getDeclaredField("currentTickTime");
        tickTime.setAccessible(true);
        tickTime.setInt(page, GameConfig.PAGE_TTL_TIME);

        assertTrue(pageProvider.triggerPageFound(player, page.getHitBoxUUID()));

        Pos spareSpot = page.getPosition();
        assertNotEquals(foundAt, spareSpot, "a found page must move to the spare spot");
        assertEquals(1.0, page.remainingTtlRatio(), "the page must get its full time on the new spot");
        ItemStack shown = ((ItemDisplayMeta) page.getEntityMeta()).getItemStack();
        assertEquals(page.getPageItem(), shown, "the page on the wall must be the one the next finder gets");

        // The pool is empty now. Had the found spot come back, the expiry would move the page onto it.
        pageProvider.triggerTTLHandling(page.getHitBoxUUID());
        assertEquals(spareSpot, page.getPosition(), "a found spot must stay used up");

        env.destroyInstance(instance, true);
    }

    @Test
    void testAFoundPageWithoutAFreeSpotIsHiddenBeforeItComesBack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        // No spare spot: the found page has nowhere else to go
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT);
        Player player = env.createPlayer(instance);

        PageEntity page = pageProvider.interactablePages().getFirst();
        Pos foundAt = page.getPosition();
        assertTrue(pageProvider.triggerPageFound(player, page.getHitBoxUUID()));

        assertEquals(foundAt, page.getPosition(), "without a free spot the page has to stay where it was found");
        assertFalse(page.isInteractable(), "the page must not be collectible again right away");
        assertFalse(pageProvider.interactablePages().contains(page));
        assertFalse(pageProvider.triggerPageFound(player, page.getHitBoxUUID()), "a click on the hidden page must not count");

        env.destroyInstance(instance, true);
    }

    /**
     * Creates a provider with {@value GameConfig#MIN_ACTIVE_PAGE_COUNT} pages placed into the instance.
     * Every spot beyond that stays free in the pool.
     */
    private static PageProvider spawnedProvider(Instance instance, int spotCount) {
        // All spots share one chunk, loaded up front: placing or teleporting a page into a chunk that
        // isn't loaded yet only completes asynchronously, and the assertions would race it.
        instance.loadChunk(0, 0).join();
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(spots(spotCount));
        pageProvider.collectStartPages(MIN_ACTIVE_PAGE_COUNT);
        pageProvider.spawn(instance);
        return pageProvider;
    }

    private static Set<PageResource> spots(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new PageResource(new Pos(i, 40, 0), Direction.NORTH))
                .collect(Collectors.toSet());
    }

    private static List<Runnable> findTasks(PageProvider pageProvider, Player player) {
        return pageProvider.interactablePages().stream()
                .map(page -> (Runnable) () -> pageProvider.triggerPageFound(player, page.getHitBoxUUID()))
                .toList();
    }

    private static String plainStatus(PageProvider pageProvider) {
        return PlainTextComponentSerializer.plainText().serialize(pageProvider.getPageStatus());
    }

    /**
     * Runs every task on its own thread, released at the same time via a shared latch, and rethrows
     * any exception a task threw so a regression fails the test instead of failing silently.
     */
    private static void runConcurrently(List<Runnable> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch start = new CountDownLatch(1);

            List<Future<?>> futures = new ArrayList<>();
            for (Runnable task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    task.run();
                }));
            }

            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testFoundPageCount(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = spawnedProvider(instance, MIN_ACTIVE_PAGE_COUNT + 1);
        Player player = env.createPlayer(instance);

        assertEquals(0, pageProvider.foundPageCount());

        pageProvider.triggerPageFound(player, pageProvider.interactablePages().getFirst().getHitBoxUUID());

        assertEquals(1, pageProvider.foundPageCount());
        env.destroyInstance(instance, true);
    }
}
