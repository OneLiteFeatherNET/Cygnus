package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapBuilderTest {

    private static final PageResource NORTH_PAGE = new PageResource(Vec.ZERO, Direction.NORTH);
    private static final PageResource SOUTH_PAGE = new PageResource(new Vec(1, 2, 3), Direction.SOUTH);

    @Test
    void defaultPoolAlwaysExistsAndCannotBeAddedOrRemoved() {
        GameMapBuilder builder = new GameMapBuilder();

        assertTrue(builder.hasPagePool("default"));
        assertFalse(builder.addPagePool("default"));
        assertFalse(builder.removePagePool("default"));
    }

    @Test
    void addedPoolIsListedAfterDefaultInAlphabeticalOrder() {
        GameMapBuilder builder = new GameMapBuilder();

        assertTrue(builder.addPagePool("nord"));
        assertTrue(builder.addPagePool("keller"));

        assertEquals(List.of("default", "keller", "nord"), builder.getPagePoolNames());
    }

    @Test
    void duplicatePoolIsRejected() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPagePool("nord");

        assertFalse(builder.addPagePool("nord"));
    }

    @Test
    void pagesGoIntoTheNamedPoolOnly() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPagePool("nord");

        assertTrue(builder.addPage("nord", Vec.ZERO, Direction.NORTH));

        assertEquals(Set.of(NORTH_PAGE), builder.getPagePool("nord"));
        assertTrue(builder.getPageFaces().isEmpty());
    }

    @Test
    void samePositionMayBeInSeveralPools() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPagePool("nord");

        assertTrue(builder.addPage("default", Vec.ZERO, Direction.NORTH));
        assertTrue(builder.addPage("nord", Vec.ZERO, Direction.NORTH));
    }

    @Test
    void buildCarriesOnlyTheDefaultPool() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPagePool("nord");
        builder.addPage(Vec.ZERO, Direction.NORTH);
        builder.addPage("nord", new Vec(1, 2, 3), Direction.SOUTH);

        assertEquals(Set.of(NORTH_PAGE), builder.build().getPageFaces());
        assertEquals(Set.of(SOUTH_PAGE), builder.getExtraPagePools().get("nord"));
    }

    @Test
    void removedPoolIsGone() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPagePool("nord");

        assertTrue(builder.removePagePool("nord"));

        assertFalse(builder.hasPagePool("nord"));
        assertEquals(List.of("default"), builder.getPagePoolNames());
    }

    @Test
    void addPageToUnknownPoolFails() {
        GameMapBuilder builder = new GameMapBuilder();

        assertFalse(builder.addPage("nord", Vec.ZERO, Direction.NORTH));
        assertFalse(builder.removePage("nord", NORTH_PAGE));
    }

    @Test
    void putPagePoolLoadsAPool() {
        GameMapBuilder builder = new GameMapBuilder();

        builder.putPagePool("nord", Set.of(NORTH_PAGE));
        assertTrue(builder.removePage("nord", NORTH_PAGE));

        assertTrue(builder.getPagePool("nord").isEmpty());
    }

    @Test
    void putPagePoolLoadsTheDefaultPool() {
        GameMapBuilder builder = new GameMapBuilder();
        builder.addPage("default", Vec.ZERO, Direction.SOUTH);

        builder.putPagePool("default", Set.of(NORTH_PAGE));

        assertEquals(Set.of(NORTH_PAGE), builder.getPagePool("default"));
        assertEquals(List.of("default"), builder.getPagePoolNames());
    }
}
