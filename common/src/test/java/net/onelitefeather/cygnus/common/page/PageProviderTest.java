package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageProviderTest {

    @Test
    void testPageTwiceLoading() {
        Set<PageResource> pageResources = Set.of(
                new PageResource(Pos.ZERO, Direction.NORTH)
        );
        PageProvider pageProvider = new PageProvider();
        assertNotNull(pageProvider);

        pageProvider.loadPageData(pageResources);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pageProvider.loadPageData(pageResources)
        );

        assertInstanceOf(IllegalArgumentException.class, exception);
        assertEquals("Can't load pages twice", exception.getMessage());
    }

    @Test
    void testEmptyPageResourceUsage() {
        PageProvider pageProvider = new PageProvider();
        assertNotNull(pageProvider);
        Set<PageResource> pageResources = Set.of();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pageProvider.loadPageData(pageResources)
        );

        assertInstanceOf(IllegalStateException.class, exception);
        assertEquals("Can't load a map without any pages", exception.getMessage());
    }

    /**
     * Reproduces the race where two players interact with the same page hitbox at almost the same time.
     * Before the fix, the losing call dereferenced a {@code null} {@link PageEntity} and threw an NPE;
     * it must now bail out silently and the winner's count must still be recorded correctly.
     */
    @Disabled(value = "Check flakiness")
    @Test
    void testConcurrentDuplicateFind(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        PageEntity pageEntity = new PageEntity(instance, Pos.ZERO, 1);
        UUID uuid = pageEntity.getHitBoxUUID();
        seedActivePages(pageProvider, pageEntity);

        Player player = env.createPlayer(instance);

        AtomicInteger completedEvents = new AtomicInteger();
        env.process().eventHandler().addListener(PageDiscoveryCompletedEvent.class, event -> completedEvents.incrementAndGet());

        runConcurrently(Collections.nCopies(8, (Runnable) () -> pageProvider.triggerPageFound(player, uuid)));

        assertEquals(1, completedEvents.get(), "the completion event must fire exactly once, not zero or more than once");
        assertEquals("1 / 1", plainStatus(pageProvider));

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the lost-update race on {@code currentFoundedPageCount}: with a plain {@code int} and
     * {@code ++}, concurrent finds of distinct pages could overwrite each other's increment and the
     * displayed count would end up below the real number found, sometimes preventing the completion
     * event from ever firing.
     */
    @Test
    void testConcurrentDistinctFinds(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        int pageCount = 6;

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(
                IntStream.range(0, pageCount)
                        .mapToObj(i -> new PageResource(new Pos(i, 0, 0), Direction.NORTH))
                        .collect(Collectors.toSet())
        );
        pageProvider.setMaxPageAmount(pageCount);

        List<PageEntity> entities = IntStream.range(0, pageCount)
                .mapToObj(i -> new PageEntity(instance, Pos.ZERO, i + 1))
                .toList();
        seedActivePages(pageProvider, entities.toArray(new PageEntity[0]));

        Player player = env.createPlayer(instance);

        AtomicInteger completedEvents = new AtomicInteger();
        env.process().eventHandler().addListener(PageDiscoveryCompletedEvent.class, event -> completedEvents.incrementAndGet());

        runConcurrently(entities.stream()
                .map(entity -> (Runnable) () -> pageProvider.triggerPageFound(player, entity.getHitBoxUUID()))
                .toList());

        assertEquals(pageCount + " / " + pageCount, plainStatus(pageProvider),
                "every concurrent find must be counted, a lost update would leave the status below " + pageCount);
        assertEquals(1, completedEvents.get(), "the completion event must fire exactly once once all pages are found");

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the race between game-end cleanup and an in-flight pickup: before the fix,
     * {@code cleanUp()} iterated the map without any guard while another thread could mutate it
     * concurrently via {@code triggerPageFound}.
     */
    @Test
    void testConcurrentCleanUp(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        int pageCount = 6;

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(
                IntStream.range(0, pageCount)
                        .mapToObj(i -> new PageResource(new Pos(i, 0, 0), Direction.NORTH))
                        .collect(Collectors.toSet())
        );
        pageProvider.setMaxPageAmount(pageCount);

        List<PageEntity> entities = IntStream.range(0, pageCount)
                .mapToObj(i -> new PageEntity(instance, Pos.ZERO, i + 1))
                .toList();
        seedActivePages(pageProvider, entities.toArray(new PageEntity[0]));

        Player player = env.createPlayer(instance);

        List<Runnable> tasks = new ArrayList<>(entities.stream()
                .map(entity -> (Runnable) () -> pageProvider.triggerPageFound(player, entity.getHitBoxUUID()))
                .toList());
        tasks.add(pageProvider::cleanUp);

        assertDoesNotThrow(() -> runConcurrently(tasks));

        env.destroyInstance(instance, true);
    }

    private static String plainStatus(PageProvider pageProvider) {
        return PlainTextComponentSerializer.plainText().serialize(pageProvider.getPageStatus());
    }

    @SuppressWarnings("unchecked")
    private static void seedActivePages(PageProvider pageProvider, PageEntity... entities) throws ReflectiveOperationException {
        Field field = PageProvider.class.getDeclaredField("activePages");
        field.setAccessible(true);
        Map<UUID, PageEntity> activePages = (Map<UUID, PageEntity>) field.get(pageProvider);
        for (PageEntity entity : entities) {
            activePages.put(entity.getHitBoxUUID(), entity);
        }
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
                    } catch (InterruptedException e) {
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
}
