package net.onelitefeather.cygnus.page;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.game.data.PageResource;
import net.onelitefeather.cygnus.page.PageProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageProviderTest {

    PageProvider pageProvider = new PageProvider(null);
    final Set<PageResource> resourceSet = new HashSet<>();

    @BeforeAll
    void prepareProvider() {
        for (int i = 0; i < 5; i++) {
            resourceSet.add(new PageResource(Pos.ZERO, "north"));
        }
        this.pageProvider.loadPageData(this.resourceSet);
    }

    @Test
    void testTriggerPageLoadTwiceToRaiseAnException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> this.pageProvider.loadPageData(this.resourceSet),
                "Can't load pages twice"
        );
    }

    @Test
    void testDataLoading() {

    }

}