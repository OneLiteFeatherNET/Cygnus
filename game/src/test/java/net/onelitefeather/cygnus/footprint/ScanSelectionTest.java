package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanSelectionTest {

    private static List<TrackPoint> line(int count) {
        List<TrackPoint> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new TrackPoint(new Pos(i * 2, 40, 0), i));
        }
        return points;
    }

    @Test
    @DisplayName("Fewer points than the maximum stay as they are")
    void underMaximum() {
        assertEquals(5, ScanSelection.select(line(5), 40, 0.7D).size());
    }

    @Test
    @DisplayName("Too many points are thinned out evenly to the maximum")
    void thins() {
        List<Pos> selected = ScanSelection.select(line(100), 40, 0.7D);

        assertEquals(40, selected.size());
        assertEquals(0.0D, selected.getFirst().x());
        assertEquals(194.0D, selected.getLast().x(), 6.0D, "the selection has to reach the end of the track");
    }

    @Test
    @DisplayName("Points closer than the spacing are only shown once")
    void spacing() {
        List<TrackPoint> points = List.of(
                new TrackPoint(new Pos(0, 40, 0), 0L),
                new TrackPoint(new Pos(0.3, 40, 0), 1L),
                new TrackPoint(new Pos(2, 40, 0), 2L));

        assertEquals(2, ScanSelection.select(points, 40, 0.7D).size());
    }

    @Test
    @DisplayName("Two points in the same block are shown once, even when they lie further apart than the spacing")
    void oneBlockOnePrint() {
        List<TrackPoint> points = List.of(
                new TrackPoint(new Pos(0.05, 40, 0.05), 0L),
                new TrackPoint(new Pos(0.95, 40, 0.95), 1L),
                new TrackPoint(new Pos(2.5, 40, 0.5), 2L));

        List<Pos> selected = ScanSelection.select(points, 40, 0.7D);

        assertEquals(2, selected.size());
        assertEquals(0.05D, selected.getFirst().x(), 1.0E-9, "the older point of the block wins");
    }
}
