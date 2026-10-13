package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScentTrailTest {

    private static final Pos SLENDER = new Pos(0.5, 40, 0.5);
    private static final Pos EAST = new Pos(100.5, 55, 0.5);

    @Test
    @DisplayName("Without spread the prints run straight towards the target, flat at the slender's feet")
    void straight() {
        List<Pos> spots = ScentTrail.spots(SLENDER, EAST, 4, 15.0D, new FixedRandom(0.5D));

        assertEquals(4, spots.size());
        for (int i = 0; i < spots.size(); i++) {
            Pos spot = spots.get(i);
            assertEquals(0.5D + ScentTrail.START_BLOCKS + i * ScentTrail.SPACING_BLOCKS, spot.x(), 1.0E-9);
            assertEquals(0.5D, spot.z(), 1.0E-9);
            assertEquals(40.0D, spot.y(), 1.0E-9);
        }
    }

    @Test
    @DisplayName("The lowest and the highest roll turn the line by the full spread to either side")
    void spread() {
        Pos left = ScentTrail.spots(SLENDER, EAST, 1, 90.0D, new FixedRandom(0.0D)).getFirst();
        Pos right = ScentTrail.spots(SLENDER, EAST, 1, 90.0D, new FixedRandom(1.0D)).getFirst();

        assertEquals(0.5D, left.x(), 1.0E-9);
        assertEquals(0.5D - ScentTrail.START_BLOCKS, left.z(), 1.0E-9);
        assertEquals(0.5D, right.x(), 1.0E-9);
        assertEquals(0.5D + ScentTrail.START_BLOCKS, right.z(), 1.0E-9);
    }

    @Test
    @DisplayName("A target right at the slender gives no direction and no prints")
    void noDirection() {
        assertEquals(List.of(), ScentTrail.spots(SLENDER, SLENDER.withY(60), 4, 15.0D, new FixedRandom(0.5D)));
    }
}
