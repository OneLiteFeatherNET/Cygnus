package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StalkStateTest {

    private static final UUID TARGET = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    /** 25 blocks away, behind and to the side of a target looking towards positive Z. */
    private static final Pos IN_THE_BAND = new Pos(15, 40, -20);

    private static SurvivorView target(double dread, boolean sees) {
        return new SurvivorView(TARGET, new Pos(0, 40, 0, 0, 0), dread, sees);
    }

    private static CreekContext at(long now, RecordingBody body, SurvivorView... survivors) {
        return Contexts.context(now, body, Contexts.route(), new ArrayList<>(), survivors);
    }

    @Test
    @DisplayName("Entering shows him to the target alone")
    void enterShowsOnlyTheTarget() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        new StalkState(TARGET, 60_000L).enter(at(0L, body, target(0.3D, false),
                new SurvivorView(OTHER, new Pos(40, 40, 40), 0.0D, false)));

        assertEquals(Set.of(TARGET), body.viewers);
    }

    @Test
    @DisplayName("Without the target he vanishes")
    void vanishesWithoutTheTarget() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        assertInstanceOf(VanishState.class, new StalkState(TARGET, 60_000L).tick(at(0L, body)));
    }

    @Test
    @DisplayName("When the time is up he vanishes")
    void vanishesWhenTheTimeIsUp() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        assertInstanceOf(VanishState.class, new StalkState(TARGET, 1000L).tick(at(1000L, body, target(0.3D, false))));
    }

    @Test
    @DisplayName("At the hunt threshold the stalk turns into a hunt")
    void turnsIntoAHunt() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        assertInstanceOf(HuntState.class, new StalkState(TARGET, 60_000L).tick(at(0L, body, target(0.6D, false))));
    }

    @Test
    @DisplayName("Seen, he lingers for a moment and then moves out of sight")
    void lingersThenMoves() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);

        state.tick(at(0L, body, target(0.3D, true)));
        state.tick(at(699L, body, target(0.3D, true)));
        assertTrue(body.teleports.isEmpty(), "he has to be seen for a moment first");

        state.tick(at(700L, body, target(0.3D, true)));

        assertEquals(1, body.teleports.size());
        Pos spot = body.position;
        double distance = spot.distance(target(0.3D, true).position());
        assertTrue(distance >= 20 - 1.0E-6 && distance <= 35 + 1.0E-6, "distance was " + distance);
        assertFalse(Contexts.SIGHT.inView(target(0.3D, true).eyes(), spot.add(0, SpotFinder.BODY_CENTRE, 0)));
    }

    @Test
    @DisplayName("When the target comes too close he moves back into the band")
    void relocatesWhenTooClose() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, -10));
        new StalkState(TARGET, 60_000L).tick(at(0L, body, target(0.3D, false)));

        assertEquals(1, body.teleports.size());
    }

    @Test
    @DisplayName("Inside the band and unseen he just stares")
    void staresInsideTheBand() {
        RecordingBody body = new RecordingBody(IN_THE_BAND);
        StalkState state = new StalkState(TARGET, 60_000L);

        assertSame(state, state.tick(at(0L, body, target(0.3D, false))));
        assertTrue(body.teleports.isEmpty());
        assertEquals(target(0.3D, false).eyes(), body.lookedAt);
    }
}
