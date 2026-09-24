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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanishStateTest {

    private static final UUID SURVIVOR = UUID.randomUUID();
    /** At the origin, looking towards positive Z. */
    private static final SurvivorView WATCHER = new SurvivorView(SURVIVOR, new Pos(0, 40, 0, 0, 0), 0.0D, false);
    private static final Pos AHEAD = new Pos(0, 40, 20);
    private static final Pos BEHIND = new Pos(0, 40, -40);

    @Test
    @DisplayName("Entering hides and calms him")
    void enterHidesHim() {
        RecordingBody body = new RecordingBody(new Pos(50, 40, 50));
        body.viewers = Set.of(SURVIVOR);
        body.aggressive = true;
        body.frozen = true;

        new VanishState(1000L).enter(Contexts.context(0L, body, Contexts.route(), new ArrayList<>(), WATCHER));

        assertTrue(body.viewers.isEmpty());
        assertFalse(body.aggressive);
        assertFalse(body.frozen);
        assertEquals(1, body.stops);
    }

    @Test
    @DisplayName("He stays away until the cooldown is over")
    void staysAwayUntilTheCooldown() {
        RecordingBody body = new RecordingBody(new Pos(50, 40, 50));
        VanishState state = new VanishState(1000L);

        assertSame(state, state.tick(Contexts.context(999L, body, Contexts.route(BEHIND), new ArrayList<>(), WATCHER)));
        assertTrue(body.teleports.isEmpty());
    }

    @Test
    @DisplayName("He comes back at a hidden point far from everyone")
    void comesBackHidden() {
        RecordingBody body = new RecordingBody(new Pos(50, 40, 50));
        VanishState state = new VanishState(1000L);

        CreekState next = state.tick(Contexts.context(1000L, body, Contexts.route(AHEAD, BEHIND), new ArrayList<>(), WATCHER));

        assertInstanceOf(WanderState.class, next);
        assertEquals(List.of(BEHIND), body.teleports);
    }

    @Test
    @DisplayName("Without a fitting point he stays away")
    void staysAwayWithoutAPoint() {
        RecordingBody body = new RecordingBody(new Pos(50, 40, 50));
        VanishState state = new VanishState(1000L);

        assertSame(state, state.tick(Contexts.context(1000L, body, Contexts.route(AHEAD), new ArrayList<>(), WATCHER)));
    }

    @Test
    @DisplayName("The cooldown shrinks as the dread grows")
    void cooldownFollowsDread() {
        assertEquals(40_000L, VanishState.cooldownMillis(Contexts.CONFIG, 0.0D));
        assertEquals(30_000L, VanishState.cooldownMillis(Contexts.CONFIG, 0.5D));
        assertEquals(20_000L, VanishState.cooldownMillis(Contexts.CONFIG, 1.0D));
    }

    @Test
    @DisplayName("Forever means forever")
    void foreverNeverEnds() {
        VanishState state = VanishState.forever();
        RecordingBody body = new RecordingBody(new Pos(50, 40, 50));

        assertTrue(state.isForever());
        assertSame(state, state.tick(Contexts.context(Long.MAX_VALUE - 1, body, Contexts.route(BEHIND), new ArrayList<>(), WATCHER)));
    }
}
