package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekVariantsTest {

    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final UUID THIRD = UUID.randomUUID();

    @Test
    @DisplayName("One variant per four survivors, at most three")
    void capacity() {
        assertEquals(0, CreekVariants.capacity(0));
        assertEquals(1, CreekVariants.capacity(1));
        assertEquals(1, CreekVariants.capacity(4));
        assertEquals(2, CreekVariants.capacity(5));
        assertEquals(2, CreekVariants.capacity(8));
        assertEquals(3, CreekVariants.capacity(9));
        assertEquals(3, CreekVariants.capacity(12));
        assertEquals(3, CreekVariants.capacity(40));
    }

    @Test
    @DisplayName("The breather after a variant shrinks as the dread grows")
    void cooldownFollowsDread() {
        assertEquals(40_000L, CreekVariants.cooldownMillis(CreekConfig.DEFAULT, 0.0D));
        assertEquals(30_000L, CreekVariants.cooldownMillis(CreekConfig.DEFAULT, 0.5D));
        assertEquals(20_000L, CreekVariants.cooldownMillis(CreekConfig.DEFAULT, 1.0D));
    }

    @Test
    @DisplayName("The survivor with the highest dread above the threshold is picked")
    void picksTheHighestDread() {
        List<SurvivorView> views = List.of(
                new SurvivorView(FIRST, new Pos(0, 40, 0), 0.3D, false),
                new SurvivorView(SECOND, new Pos(10, 40, 0), 0.5D, false),
                new SurvivorView(THIRD, new Pos(20, 40, 0), 0.1D, false));

        assertEquals(Optional.of(views.get(1)), CreekVariants.pick(views, 0.25D, _ -> true));
    }

    @Test
    @DisplayName("Nobody above the threshold, nobody is picked")
    void picksNobodyBelowTheThreshold() {
        List<SurvivorView> views = List.of(new SurvivorView(FIRST, new Pos(0, 40, 0), 0.2D, false));

        assertEquals(Optional.empty(), CreekVariants.pick(views, 0.25D, _ -> true));
    }

    @Test
    @DisplayName("Survivors that are not eligible are skipped")
    void skipsIneligibleSurvivors() {
        List<SurvivorView> views = List.of(
                new SurvivorView(FIRST, new Pos(0, 40, 0), 0.9D, false),
                new SurvivorView(SECOND, new Pos(10, 40, 0), 0.5D, false));

        assertEquals(Optional.of(views.get(1)), CreekVariants.pick(views, 0.25D, id -> !id.equals(FIRST)));
    }

    @Test
    @DisplayName("On a tie the one who strayed from the group is picked")
    void prefersTheLonelyOne() {
        List<SurvivorView> views = List.of(
                new SurvivorView(FIRST, new Pos(0, 40, -60), 0.3D, false),
                new SurvivorView(SECOND, new Pos(5, 40, -60), 0.3D, false),
                new SurvivorView(THIRD, new Pos(0, 40, -160), 0.3D, false));

        assertEquals(THIRD, CreekVariants.pick(views, 0.25D, _ -> true).orElseThrow().id());
    }
}
