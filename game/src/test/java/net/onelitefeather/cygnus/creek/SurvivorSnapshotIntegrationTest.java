package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivorSnapshotIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The snapshot rates each survivor, and nobody sees a creek yet")
    void takesOneViewPerSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(10, 40, 0));

        SurvivorSnapshot survivors = SurvivorSnapshot.take(List.of(first, second),
                (id, _) -> id.equals(first.getUuid()) ? 0.7D : 0.2D, new CreekRespite(), 0L);

        assertEquals(List.of(first, second), survivors.players());
        SurvivorView view = survivors.views().getFirst();
        assertEquals(first.getUuid(), view.id());
        assertEquals(first.getPosition(), view.position());
        assertEquals(0.7D, view.dread());
        assertEquals(0.2D, survivors.views().get(1).dread());
        assertFalse(survivors.views().stream().anyMatch(SurvivorView::seesCreek));
    }

    @Test
    @DisplayName("Every survivor is rated at the time of the step")
    void ratesAtTheStepTime(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(10, 40, 0));

        SurvivorSnapshot survivors = SurvivorSnapshot.take(List.of(first, second),
                (_, now) -> now / 1000.0D, new CreekRespite(), 500L);

        assertEquals(0.5D, survivors.views().getFirst().dread());
        assertEquals(0.5D, survivors.views().get(1).dread());
    }

    @Test
    @DisplayName("A survivor is found by id, anyone else is not")
    void findsPlayers(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        SurvivorSnapshot survivors = SurvivorSnapshot.take(List.of(first), (_, _) -> 0.0D, new CreekRespite(), 0L);

        assertSame(first, survivors.player(first.getUuid()));
        assertNull(survivors.player(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Players and views have to match up")
    void rejectsMismatchedViews(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));

        assertThrows(IllegalArgumentException.class, () -> new SurvivorSnapshot(List.of(first), List.of()));
    }

    @Test
    @DisplayName("A survivor the respite spares is marked as spared until it runs out")
    void marksSparedSurvivors(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(10, 40, 0));
        CreekRespite respite = new CreekRespite();
        respite.spare(first.getUuid(), 1000L);

        SurvivorSnapshot during = SurvivorSnapshot.take(List.of(first, second), (_, _) -> 0.0D, respite, 999L);
        SurvivorSnapshot after = SurvivorSnapshot.take(List.of(first, second), (_, _) -> 0.0D, respite, 1000L);

        assertTrue(during.views().getFirst().spared());
        assertFalse(during.views().get(1).spared());
        assertFalse(after.views().getFirst().spared());
    }
}
