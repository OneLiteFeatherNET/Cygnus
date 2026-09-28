package net.onelitefeather.cygnus.creek.dread;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageProgressDreadTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Pos HERE = new Pos(0, 40, 0);
    private static final List<Pos> NEIGHBOUR = List.of(new Pos(5, 40, 0));

    private static PageProgressDread dread(int found, int max, long elapsedMillis, int gameTimeSeconds) {
        return new PageProgressDread(() -> found, () -> max, () -> elapsedMillis, gameTimeSeconds, CreekConfig.DEFAULT);
    }

    @Test
    @DisplayName("Nothing found, no time gone, not alone: no dread")
    void nothingYet() {
        assertEquals(0.0D, dread(0, 8, 0, 900).dreadOf(ID, HERE, NEIGHBOUR), 1.0E-9);
    }

    @Test
    @DisplayName("Every page found weighs 0.6")
    void allPages() {
        assertEquals(0.6D, dread(8, 8, 0, 900).dreadOf(ID, HERE, NEIGHBOUR), 1.0E-9);
    }

    @Test
    @DisplayName("Half the round gone weighs 0.15")
    void halfTheRound() {
        assertEquals(0.15D, dread(0, 8, 450_000, 900).dreadOf(ID, HERE, NEIGHBOUR), 1.0E-9);
    }

    @Test
    @DisplayName("Nobody within the radius adds 0.1")
    void aloneAddsIsolation() {
        assertEquals(0.1D, dread(0, 8, 0, 900).dreadOf(ID, HERE, List.of(new Pos(30, 40, 0))), 1.0E-9);
    }

    @Test
    @DisplayName("The last survivor is always alone")
    void lastSurvivorIsAlone() {
        assertEquals(0.1D, dread(0, 8, 0, 900).dreadOf(ID, HERE, List.of()), 1.0E-9);
    }

    @Test
    @DisplayName("Everything at once is full dread")
    void everything() {
        assertEquals(1.0D, dread(8, 8, 900_000, 900).dreadOf(ID, HERE, List.of()), 1.0E-9);
    }

    @Test
    @DisplayName("Values past their maximum are clamped")
    void clamped() {
        assertEquals(1.0D, dread(12, 8, 2_000_000, 900).dreadOf(ID, HERE, List.of()), 1.0E-9);
    }

    @Test
    @DisplayName("Without pages or round time nothing divides by zero")
    void noDivisionByZero() {
        assertEquals(0.0D, dread(0, 0, 0, 0).dreadOf(ID, HERE, NEIGHBOUR), 1.0E-9);
    }
}
