package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivorTrackLogTest {

    private final UUID survivor = UUID.randomUUID();
    private final SurvivorTrackLog log = new SurvivorTrackLog(FootprintConfig.DEFAULT);

    /** Walks one block per move along x, starting at x 0. */
    private void walk(int blocks, long now) {
        for (int x = 0; x < blocks; x++) {
            this.log.moved(this.survivor, new Pos(x, 40, 0), new Pos(x + 1, 40, 0), now);
        }
    }

    @Test
    @DisplayName("A point is recorded every two blocks")
    void samples() {
        walk(1, 0L);
        assertEquals(0, this.log.size(this.survivor));

        walk(4, 0L);
        assertEquals(2, this.log.size(this.survivor));
    }

    @Test
    @DisplayName("Points older than the history are dropped")
    void ages() {
        walk(2, 0L);
        this.log.moved(this.survivor, new Pos(10, 40, 0), new Pos(11, 40, 0), 30_001L);
        this.log.moved(this.survivor, new Pos(11, 40, 0), new Pos(12, 40, 0), 30_001L);

        assertEquals(1, this.log.size(this.survivor));
    }

    @Test
    @DisplayName("A teleport records no point")
    void teleport() {
        this.log.moved(this.survivor, new Pos(0, 40, 0), new Pos(30, 40, 0), 0L);

        assertEquals(0, this.log.size(this.survivor));
    }

    @Test
    @DisplayName("Only points inside the radius and before the gap are returned")
    void pointsNear() {
        walk(2, 1_000L);                                                      // point at x 2, t 1 s
        this.log.moved(this.survivor, new Pos(2, 40, 0), new Pos(4, 40, 0), 9_000L);   // point at x 4, t 9 s
        this.log.moved(this.survivor, new Pos(4, 40, 0), new Pos(6, 40, 0), 1_000L);   // point at x 6, t 1 s

        List<TrackPoint> found = this.log.pointsNear(new Pos(0, 40, 0), 5.0D, 5_000L, 10_000L);

        assertEquals(1, found.size(), "x 4 is too new and x 6 is outside the radius");
        assertEquals(2.0D, found.getFirst().position().x());
    }

    @Test
    @DisplayName("Clearing a survivor forgets their points")
    void clear() {
        walk(4, 0L);

        this.log.clear(this.survivor);

        assertEquals(0, this.log.size(this.survivor));
        assertTrue(this.log.pointsNear(new Pos(0, 40, 0), 40.0D, 0L, 0L).isEmpty());
    }
}
