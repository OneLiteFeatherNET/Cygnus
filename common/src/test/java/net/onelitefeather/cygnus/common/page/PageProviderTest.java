package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageProviderTest {

    @Test
    void testPageTwiceLoading() {
        Set<PageResource> pageResources = Set.of(
                new PageResource(Pos.ZERO, Direction.NORTH)
        );
        PageProvider pageProvider = new PageProvider(() -> {});
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
        PageProvider pageProvider = new PageProvider(() -> {});
        assertNotNull(pageProvider);
        Set<PageResource> pageResources = Set.of();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pageProvider.loadPageData(pageResources)
        );

        assertInstanceOf(IllegalStateException.class, exception);
        assertEquals("Can't load a map without any pages", exception.getMessage());
    }
}
