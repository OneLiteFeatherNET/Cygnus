package net.onelitefeather.cygnus.creek.consequence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchLaunchTest {

    @Test
    @DisplayName("The launch speed carries the survivor to the wanted apex")
    void speedReachesTheApex() {
        for (double height : new double[]{1.0D, 5.0D, 12.5D, 20.0D}) {
            double perSecond = CatchLaunch.launchSpeed(height);

            assertEquals(height, CatchLaunch.apexOf(perSecond / 20.0D), 1.0E-6D,
                    "a launch of " + height + " blocks must peak at " + height);
        }
    }

    @Test
    @DisplayName("Five blocks need about one block per tick, as in vanilla physics")
    void fiveBlocksSpeedIsPlausible() {
        double perTick = CatchLaunch.launchSpeed(5.0D) / 20.0D;

        assertTrue(perTick > 0.9D && perTick < 1.2D, "was " + perTick);
    }

    @Test
    @DisplayName("A higher launch is faster")
    void higherIsFaster() {
        assertTrue(CatchLaunch.launchSpeed(8.0D) > CatchLaunch.launchSpeed(5.0D));
    }

    @Test
    @DisplayName("No height needs no speed")
    void zeroHeightIsZeroSpeed() {
        assertEquals(0.0D, CatchLaunch.launchSpeed(0.0D), 0.0D);
    }
}
