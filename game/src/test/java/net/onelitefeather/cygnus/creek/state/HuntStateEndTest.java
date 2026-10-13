package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers how a hunt says why it ended, which is what the hunt outcome is made from.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class HuntStateEndTest {

    private static final UUID TARGET = UUID.randomUUID();
    private static final Pos TARGET_POS = new Pos(0, 40, 0, 180, 0);

    private static CreekContext at(long now, RecordingBody body, UUID who) {
        return Contexts.context(now, body, Contexts.route(), new ArrayList<>(),
                new SurvivorView(who, TARGET_POS, 0.7D, false));
    }

    @Test
    @DisplayName("A running hunt has no end yet")
    void runningHuntHasNoEnd() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(0L, new RecordingBody(new Pos(0, 40, 10)), TARGET));

        assertNull(hunt.end());
    }

    @Test
    @DisplayName("A catch ends the hunt as caught")
    void catchIsCaught() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(0L, new RecordingBody(new Pos(0, 40, 1)), TARGET));

        assertEquals(HuntEnd.CAUGHT, hunt.end());
    }

    @Test
    @DisplayName("Running out of time ends the hunt as a timeout")
    void timeIsUp() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(30_000L, new RecordingBody(new Pos(0, 40, 10)), TARGET));

        assertEquals(HuntEnd.TIMEOUT, hunt.end());
    }

    @Test
    @DisplayName("A target that is no longer among the survivors ends the hunt as gone")
    void targetIsGone() {
        HuntState hunt = new HuntState(TARGET, 30_000L);

        hunt.tick(at(0L, new RecordingBody(new Pos(0, 40, 10)), UUID.randomUUID()));

        assertEquals(HuntEnd.GONE, hunt.end());
    }
}
