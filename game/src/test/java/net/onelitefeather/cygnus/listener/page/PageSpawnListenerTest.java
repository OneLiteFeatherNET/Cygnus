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
        assertEquals("Active instance not available for page spawning", exception.getMessage());
    }

    @Test
    void acceptPlacesTheCollectedPagesIntoTheActiveInstance(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(
                IntStream.range(0, MIN_ACTIVE_PAGE_COUNT)
                        .mapToObj(i -> new PageResource(new Pos(i, 40, 0), Direction.NORTH))
                        .collect(Collectors.toSet())
        );
        pageProvider.collectStartPages(MIN_ACTIVE_PAGE_COUNT);

        new PageSpawnListener(pageProvider, () -> instance).accept(new PageSpawnEvent());

        assertEquals(MIN_ACTIVE_PAGE_COUNT, pageProvider.interactablePages().size());
        assertTrue(pageProvider.interactablePages().stream().allMatch(page -> page.getInstance() == instance),
                "every collected page must be placed into the active instance");

        env.destroyInstance(instance, true);
    }
}
