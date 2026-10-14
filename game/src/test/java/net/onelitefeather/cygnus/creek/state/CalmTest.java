package net.onelitefeather.cygnus.creek.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalmTest {

    @Test
    @DisplayName("A full round is not calm at all, the last survivor gets the full calm")
    void followsTheRound() {
        assertEquals(0.0D, Calm.of(8, 8).value());
        assertEquals(1.0D, Calm.of(8, 1).value());
        assertEquals(4.0D / 7.0D, Calm.of(8, 4).value(), 1.0E-9);
    }

    @Test
    @DisplayName("A round that starts with one survivor has no course to follow")
    void aSingleStarterStaysUncalm() {
        assertEquals(0.0D, Calm.of(1, 1).value());
        assertEquals(0.0D, Calm.of(0, 0).value());
    }

    @Test
    @DisplayName("More survivors than at the start count as a full round")
    void moreThanAtTheStartIsAFullRound() {
        assertEquals(0.0D, Calm.of(4, 6).value());
    }

    @Test
    @DisplayName("Without calm the speed and the waits stay as they are")
    void noCalmChangesNothing() {
        assertEquals(0.25D, Calm.NONE.speed(0.25D));
        assertEquals(3000L, Calm.NONE.stretch(3000L));
        assertEquals(35.0D, Calm.NONE.stretch(35.0D));
    }

    @Test
    @DisplayName("At full calm he moves at 80 percent and waits half as long again")
    void fullCalm() {
        Calm calm = Calm.of(8, 1);
        assertEquals(0.2D, calm.speed(0.25D), 1.0E-9);
        assertEquals(4500L, calm.stretch(3000L));
        assertEquals(52.5D, calm.stretch(35.0D), 1.0E-9);
    }

    @Test
    @DisplayName("Half way through the round he is half as calm")
    void halfCalm() {
        Calm calm = new Calm(0.5D);
        assertEquals(0.225D, calm.speed(0.25D), 1.0E-9);
        assertEquals(3750L, calm.stretch(3000L));
    }
}
