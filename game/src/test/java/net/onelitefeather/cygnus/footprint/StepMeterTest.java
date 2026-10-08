package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepMeterTest {

    @Test
    @DisplayName("A step completes once the walked distance reaches the step length")
    void completesStep() {
        StepMeter meter = new StepMeter(3.0D, 8.0D);

        assertFalse(meter.advance(new Pos(0, 40, 0), new Pos(1, 40, 0)));
        assertFalse(meter.advance(new Pos(1, 40, 0), new Pos(2, 40, 0)));
        assertTrue(meter.advance(new Pos(2, 40, 0), new Pos(3, 40, 0)));
        assertFalse(meter.advance(new Pos(3, 40, 0), new Pos(4, 40, 0)), "the meter starts again after a step");
    }

    @Test
    @DisplayName("Only horizontal movement counts")
    void ignoresVertical() {
        StepMeter meter = new StepMeter(3.0D, 8.0D);

        assertFalse(meter.advance(new Pos(0, 40, 0), new Pos(0, 45, 0)));
        assertFalse(meter.advance(new Pos(0, 45, 0), new Pos(0, 40, 0)));
    }

    @Test
    @DisplayName("A teleport is not walked distance")
    void ignoresTeleport() {
        StepMeter meter = new StepMeter(3.0D, 8.0D);

        assertFalse(meter.advance(new Pos(0, 40, 0), new Pos(20, 40, 0)));
        assertFalse(meter.advance(new Pos(20, 40, 0), new Pos(22, 40, 0)), "the teleport must not count towards the step");
    }

    @Test
    @DisplayName("A reset drops the distance walked so far")
    void reset() {
        StepMeter meter = new StepMeter(3.0D, 8.0D);
        meter.advance(new Pos(0, 40, 0), new Pos(2, 40, 0));

        meter.reset();

        assertFalse(meter.advance(new Pos(2, 40, 0), new Pos(3, 40, 0)));
    }
}
