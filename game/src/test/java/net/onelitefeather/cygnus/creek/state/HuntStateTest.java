package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuntStateTest {

    private static final UUID TARGET = UUID.randomUUID();
    /** At the origin, looking towards negative Z - away from where he comes from. */
    private static final Pos TARGET_POS = new Pos(0, 40, 0, 180, 0);

    private static SurvivorView target(boolean sees) {
        return new SurvivorView(TARGET, TARGET_POS, 0.7D, sees);
    }

    private static CreekContext at(long now, RecordingBody body, List<UUID> caught, boolean sees) {
        return Contexts.context(now, body, Contexts.route(), caught, target(sees));
    }

    @Test
    @DisplayName("Entering shows him to the target and lights his eyes")
    void enterLightsTheEyes() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        new HuntState(TARGET, 30_000L).enter(at(0L, body, new ArrayList<>(), false));

        assertEquals(Set.of(TARGET), body.viewers);
        assertTrue(body.aggressive);
    }

    @Test
    @DisplayName("Seen, he freezes")
    void freezesWhenSeen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntState state = new HuntState(TARGET, 30_000L);

        assertSame(state, state.tick(at(0L, body, new ArrayList<>(), true)));
        assertTrue(body.frozen);
        assertNull(body.goal);
    }

    @Test
    @DisplayName("Unseen, he closes in")
    void closesInWhenUnseen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        new HuntState(TARGET, 30_000L).tick(at(0L, body, new ArrayList<>(), false));

        assertFalse(body.frozen);
        assertEquals(TARGET_POS, body.goal);
        assertEquals(Contexts.CONFIG.huntSpeed(), body.speed);
    }

    @Test
    @DisplayName("Close enough and unseen, he catches the target")
    void catchesTheTarget() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();

        CreekState next = new HuntState(TARGET, 30_000L).tick(at(0L, body, caught, false));

        assertEquals(List.of(TARGET), caught);
        assertInstanceOf(VanishState.class, next);
    }

    @Test
    @DisplayName("Close enough but seen, he does not")
    void noCatchWhileSeen() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 1));
        List<UUID> caught = new ArrayList<>();

        new HuntState(TARGET, 30_000L).tick(at(0L, body, caught, true));

        assertTrue(caught.isEmpty());
    }

    @Test
    @DisplayName("When the time is up he gives up")
    void givesUp() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        assertInstanceOf(VanishState.class, new HuntState(TARGET, 30_000L).tick(at(30_000L, body, new ArrayList<>(), false)));
    }

    @Test
    @DisplayName("Stuck, he takes a shortcut close to the target")
    void takesAShortcut() {
        RecordingBody body = new RecordingBody(new Pos(0, 40, 10));
        HuntState state = new HuntState(TARGET, 30_000L);
        state.enter(at(0L, body, new ArrayList<>(), false));

        state.tick(at(0L, body, new ArrayList<>(), false));
        state.tick(at(3000L, body, new ArrayList<>(), false));

        assertEquals(1, body.teleports.size());
        double distance = body.position.distance(TARGET_POS);
        assertTrue(distance >= HuntState.NEAR_MIN - 1.0E-6 && distance <= HuntState.NEAR_MAX + 1.0E-6, "distance was " + distance);
    }
}
