package net.onelitefeather.cygnus.listener.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.onelitefeather.cygnus.common.config.GameConfig.MIN_ACTIVE_PAGE_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageSpawnListenerTest {

    @Test
    void acceptThrowsWhenActiveInstanceIsUnavailable() {
        PageProvider pageProvider = new PageProvider();
        PageSpawnListener listener = new PageSpawnListener(pageProvider, () -> null);
        PageSpawnEvent event = new PageSpawnEvent();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.accept(event)
        );
        assertEquals("Active instance not available for page collection", exception.getMessage());
    }

    @Test
    void acceptCollectsThePagesForTheActiveInstanceBeforeSpawning(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(
                IntStream.range(0, MIN_ACTIVE_PAGE_COUNT)
                        .mapToObj(i -> new PageResource(new Pos(i, 0, 0), Direction.NORTH))
                        .collect(Collectors.toSet())
        );

        PageSpawnListener listener = new PageSpawnListener(pageProvider, () -> instance);

        assertEquals(0, activePageCount(pageProvider), "no pages should exist before the event is handled");

        assertDoesNotThrow(() -> listener.accept(new PageSpawnEvent()));

        assertEquals(MIN_ACTIVE_PAGE_COUNT, activePageCount(pageProvider),
                "collectStartPages must have run so spawn() has something to spawn");

        env.destroyInstance(instance, true);
    }

    @SuppressWarnings("unchecked")
    private static int activePageCount(PageProvider pageProvider) throws ReflectiveOperationException {
        Field field = PageProvider.class.getDeclaredField("activePages");
        field.setAccessible(true);
        Map<UUID, ?> activePages = (Map<UUID, ?>) field.get(pageProvider);
        return activePages.size();
    }
}
