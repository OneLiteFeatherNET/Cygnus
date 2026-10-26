package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageGroupsTest {

    private static PageResource spot(double x, double y, double z) {
        return new PageResource(new Vec(x, y, z), Direction.NORTH);
    }

    @Test
    @DisplayName("No spots make no groups")
    void noSpotsNoGroups() {
        assertTrue(PageGroups.find(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Spots within the link distance form one group")
    void closeSpotsShareAGroup() {
        PageResource a = spot(0, 40, 0);
        PageResource b = spot(PageGroups.LINK_DISTANCE, 40, 0);

        List<PageGroups.Group> groups = PageGroups.find(List.of(a, b));

        assertEquals(1, groups.size());
        assertEquals(Set.of(a, b), Set.copyOf(groups.getFirst().spots()));
    }

    @Test
    @DisplayName("A chain of close spots is one group, even if its ends are far apart")
    void chainsLinkUp() {
        List<PageResource> chain = List.of(spot(0, 40, 0), spot(8, 40, 0), spot(16, 40, 0), spot(24, 40, 0));

        assertEquals(1, PageGroups.find(chain).size());
    }

    @Test
    @DisplayName("A spot beyond the link distance stands on its own")
    void farSpotIsItsOwnGroup() {
        PageResource lone = spot(0, 40, PageGroups.LINK_DISTANCE + 0.5D);

        List<PageGroups.Group> groups = PageGroups.find(List.of(spot(0, 40, 0), lone));

        assertEquals(2, groups.size());
    }

    @Test
    @DisplayName("Height counts, so a spot high above is not linked")
    void heightCounts() {
        assertEquals(2, PageGroups.find(List.of(spot(0, 40, 0), spot(0, 40 + PageGroups.LINK_DISTANCE + 1, 0))).size());
    }

    @Test
    @DisplayName("Groups come largest first and know their share of all spots")
    void largestFirstWithShare() {
        List<PageResource> spots = List.of(spot(100, 40, 0), spot(0, 40, 0), spot(2, 40, 0), spot(4, 40, 0));

        List<PageGroups.Group> groups = PageGroups.find(spots);

        assertEquals(List.of(3, 1), groups.stream().map(group -> group.spots().size()).toList());
        assertEquals(0.75D, groups.getFirst().share());
        assertEquals(0.25D, groups.getLast().share());
    }

    @Test
    @DisplayName("The centre of a group is the middle of its spots")
    void centreIsTheMiddle() {
        PageGroups.Group group = PageGroups.find(List.of(spot(0, 40, 0), spot(4, 42, 2))).getFirst();

        assertEquals(new Vec(2, 41, 1), group.centre());
    }

    @Test
    @DisplayName("A group holding more than a quarter of the spots is crowded, a lone spot never")
    void crowdedGroups() {
        List<PageResource> spots = List.of(spot(0, 40, 0), spot(2, 40, 0),
                spot(100, 40, 0), spot(200, 40, 0), spot(300, 40, 0));

        List<PageGroups.Group> groups = PageGroups.find(spots);

        assertTrue(groups.getFirst().crowded(), "two of five spots are 40 %");
        assertFalse(groups.getLast().crowded(), "a lone spot cannot crowd anything");
    }

    @Test
    @DisplayName("A group with exactly a quarter of the spots is not crowded")
    void aQuarterIsFine() {
        List<PageResource> spots = List.of(spot(0, 40, 0), spot(2, 40, 0),
                spot(100, 40, 0), spot(200, 40, 0), spot(300, 40, 0), spot(400, 40, 0),
                spot(500, 40, 0), spot(600, 40, 0));

        assertFalse(PageGroups.find(spots).getFirst().crowded());
    }
}
