package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekConfigTest {

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaultsMatchTheDesign() {
        CreekConfig config = CreekConfig.DEFAULT;

        assertTrue(config.enabled());
        assertTrue(config.activeWithLastSurvivor());
        assertEquals(48, config.sightRange());
        assertEquals(0.25D, config.stalkThreshold());
        assertEquals(0.6D, config.huntThreshold());
        assertEquals(20, config.stalkMinDistance());
        assertEquals(35, config.stalkMaxDistance());
        assertEquals(40, config.stalkMinAngle());
        assertEquals(70, config.stalkMaxAngle());
        assertEquals(1.5D, config.catchDistance());
        assertEquals(15, config.personalSpace());
        assertEquals(2, config.betrayalCatchCount());
        assertEquals(0.15D, config.betrayalChance());
        assertEquals(3.0D, config.routeLinkDistance());
    }

    @Test
    @DisplayName("The link distance has to be above 0")
    void linkDistanceAboveZero() {
        CreekConfig config = CreekConfig.DEFAULT;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new CreekConfig(
                config.enabled(), config.activeWithLastSurvivor(), config.sightRange(), config.sightViewAngle(),
                config.wanderPauseMillis(), config.wanderSpeed(), config.huntSpeed(), config.stalkThreshold(), config.huntThreshold(),
                config.stalkMinDistance(), config.stalkMaxDistance(), config.stalkMinAngle(), config.stalkMaxAngle(),
                config.stalkRevealMillis(), config.stalkMinSeconds(), config.stalkMaxSeconds(), config.huntMaxSeconds(),
                config.catchDistance(), config.vanishMinSeconds(), config.vanishMaxSeconds(), config.respawnMinDistance(),
                config.personalSpace(), config.stuckMillis(), config.dreadPageWeight(), config.dreadTimeWeight(),
                config.dreadIsolationWeight(), config.isolationRadius(), config.betrayalCatchCount(),
                config.betrayalChance(), config.betrayalGlowSeconds(), config.slownessSeconds(), 0.0D));
        assertTrue(exception.getMessage().contains("routeLinkDistance"));
    }

    @Test
    @DisplayName("The stalk threshold has to stay below the hunt threshold")
    void stalkThresholdBelowHunt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> with(0.6D, 0.6D, 35, 40, 15));
        assertTrue(exception.getMessage().contains("huntThreshold"));
    }

    @Test
    @DisplayName("A hiding place inside the cone is rejected")
    void hidingPlaceOutsideTheCone() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> with(0.25D, 0.6D, 40, 40, 15));
        assertTrue(exception.getMessage().contains("stalkMinAngle"));
    }

    @Test
    @DisplayName("The personal space has to fit inside the stalk band")
    void personalSpaceInsideTheBand() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> with(0.25D, 0.6D, 35, 40, 20));
        assertTrue(exception.getMessage().contains("stalkMinDistance"));
    }

    private static CreekConfig with(double stalkThreshold, double huntThreshold, int sightViewAngle,
                                      int stalkMinAngle, int personalSpace) {
        CreekConfig d = CreekConfig.DEFAULT;
        return new CreekConfig(
                d.enabled(), d.activeWithLastSurvivor(), d.sightRange(), sightViewAngle,
                d.wanderPauseMillis(), d.wanderSpeed(), d.huntSpeed(), stalkThreshold, huntThreshold,
                d.stalkMinDistance(), d.stalkMaxDistance(), stalkMinAngle, d.stalkMaxAngle(),
                d.stalkRevealMillis(), d.stalkMinSeconds(), d.stalkMaxSeconds(), d.huntMaxSeconds(),
                d.catchDistance(), d.vanishMinSeconds(), d.vanishMaxSeconds(), d.respawnMinDistance(),
                personalSpace, d.stuckMillis(), d.dreadPageWeight(), d.dreadTimeWeight(),
                d.dreadIsolationWeight(), d.isolationRadius(), d.betrayalCatchCount(),
                d.betrayalChance(), d.betrayalGlowSeconds(), d.slownessSeconds(), d.routeLinkDistance());
    }

    @Test
    @DisplayName("Hunting, he is faster than a walking survivor")
    void huntIsFasterThanWalking() {
        // A walking player covers about 0.216 blocks per tick; the speed attribute is blocks per tick.
        assertTrue(CreekConfig.DEFAULT.huntSpeed() > 0.216D);
    }
}
