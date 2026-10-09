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
        CreekConfig defaults = CreekConfig.DEFAULT;

        assertTrue(defaults.enabled());
        assertTrue(defaults.activeWithLastSurvivor());
        assertEquals(48, defaults.sightRange());
        assertEquals(0.25D, defaults.stalkThreshold());
        assertEquals(0.6D, defaults.huntThreshold());
        assertEquals(20, defaults.stalkMinDistance());
        assertEquals(35, defaults.stalkMaxDistance());
        assertEquals(40, defaults.stalkMinAngle());
        assertEquals(70, defaults.stalkMaxAngle());
        assertEquals(10, defaults.huntMinStalkSeconds());
        assertEquals(45, defaults.huntCooldownSeconds());
        assertEquals(1.5D, defaults.catchDistance());
        assertEquals(15, defaults.personalSpace());
        assertEquals(2, defaults.betrayalCatchCount());
        assertEquals(0.15D, defaults.betrayalChance());
        assertEquals(3.0D, defaults.routeLinkDistance());
        assertEquals(0.15D, defaults.randomStopChance());
        assertEquals(1500, defaults.randomStopMinMillis());
        assertEquals(4000, defaults.randomStopMaxMillis());
    }

    @Test
    @DisplayName("The link distance has to be above 0")
    void linkDistanceAboveZero() {
        CreekConfig defaults = CreekConfig.DEFAULT;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), 0.0D,
                defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis(), defaults.launchHeight(), defaults.swapChance(), defaults.launchDamage()));
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
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), sightViewAngle,
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), stalkThreshold, huntThreshold,
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), stalkMinAngle, defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                personalSpace, defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis(), defaults.launchHeight(), defaults.swapChance(), defaults.launchDamage());
    }

    @Test
    @DisplayName("Hunting, he is faster than a walking survivor")
    void huntIsFasterThanWalking() {
        // A walking player covers about 0.216 blocks per tick; the speed attribute is blocks per tick.
        assertTrue(CreekConfig.DEFAULT.huntSpeed() > 0.216D);
    }

    private static CreekConfig withRandomStops(double chance, int minMillis, int maxMillis) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                chance, minMillis, maxMillis, defaults.launchHeight(), defaults.swapChance(), defaults.launchDamage());
    }

    @Test
    @DisplayName("Random stops need a chance between 0 and 1 and a range that does not run backwards")
    void randomStopsAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> withRandomStops(1.5D, 1500, 4000));
        assertThrows(IllegalArgumentException.class, () -> withRandomStops(0.15D, -1, 4000));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> withRandomStops(0.15D, 5000, 4000));
        assertTrue(exception.getMessage().contains("randomStopMinMillis"));
        withRandomStops(0.0D, 0, 0);
    }

    @Test
    @DisplayName("The launch height is 5 blocks by default, between 0 and 20, and 0 turns it off")
    void launchHeightIsChecked() {
        assertEquals(5.0D, CreekConfig.DEFAULT.launchHeight());
        assertThrows(IllegalArgumentException.class, () -> withLaunchHeight(-0.1D));
        assertThrows(IllegalArgumentException.class, () -> withLaunchHeight(20.1D));
        withLaunchHeight(0.0D);
        withLaunchHeight(20.0D);
    }

    private static CreekConfig withLaunchHeight(double height) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis(), height, defaults.swapChance(), defaults.launchDamage());
    }

    @Test
    @DisplayName("The swap chance is 0.5 by default and must lie between 0 and 1")
    void swapChanceIsChecked() {
        assertEquals(0.5D, CreekConfig.DEFAULT.swapChance());
        assertThrows(IllegalArgumentException.class, () -> withSwapChance(-0.1D));
        assertThrows(IllegalArgumentException.class, () -> withSwapChance(1.1D));
        withSwapChance(0.0D);
        withSwapChance(1.0D);
    }

    private static CreekConfig withSwapChance(double chance) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis(), defaults.launchHeight(), chance, defaults.launchDamage());
    }

    @Test
    @DisplayName("The launch damage is 4 by default, between 0 and 20, and 0 turns it off")
    void launchDamageIsChecked() {
        assertEquals(4.0D, CreekConfig.DEFAULT.launchDamage());
        assertThrows(IllegalArgumentException.class, () -> withLaunchDamage(-0.1D));
        assertThrows(IllegalArgumentException.class, () -> withLaunchDamage(20.1D));
        withLaunchDamage(0.0D);
        withLaunchDamage(20.0D);
    }

    private static CreekConfig withLaunchDamage(double damage) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), defaults.activeWithLastSurvivor(), defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis(), defaults.launchHeight(), defaults.swapChance(), damage);
    }
}
