package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekConfigTest {

    private static final CreekConfig DEFAULTS = CreekConfig.DEFAULT;

    private static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }

    /** The defaults with other top-level values and the given stalk, sight and hunt groups. */
    private static CreekConfig with(double routeLinkDistance, int personalSpace, CreekConfig.Sight sight,
                                    CreekConfig.Stalk stalk, CreekConfig.Hunt hunt) {
        return new CreekConfig(DEFAULTS.enabled(), DEFAULTS.activeWithLastSurvivor(), routeLinkDistance,
                personalSpace, DEFAULTS.stuckMillis(), sight, DEFAULTS.wander(), stalk, hunt,
                DEFAULTS.vanish(), DEFAULTS.catching());
    }

    /** The default stalk group with another threshold, minimum distance and minimum angle. */
    private static CreekConfig.Stalk stalk(double threshold, int minDistance, int minAngle) {
        CreekConfig.Stalk stalk = DEFAULTS.stalk();
        return new CreekConfig.Stalk(threshold, minDistance, stalk.maxDistance(), minAngle, stalk.maxAngle(),
                stalk.revealMillis(), stalk.minSeconds(), stalk.maxSeconds());
    }

    /** The default hunt group with another threshold. */
    private static CreekConfig.Hunt hunt(double threshold) {
        CreekConfig.Hunt hunt = DEFAULTS.hunt();
        return new CreekConfig.Hunt(hunt.speed(), threshold, hunt.maxSeconds(), hunt.minStalkSeconds(),
                hunt.cooldownSeconds(), hunt.catchDistance());
    }

    /** The default catch group with another launch height, swap chance and launch damage. */
    private static CreekConfig.Catch catching(double launchHeight, double swapChance, double launchDamage) {
        CreekConfig.Catch catching = DEFAULTS.catching();
        return new CreekConfig.Catch(catching.betrayalCatchCount(), catching.betrayalChance(),
                catching.betrayalGlowSeconds(), catching.slownessSeconds(), launchHeight, swapChance, launchDamage);
    }

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaultsMatchTheDesign() {
        assertTrue(DEFAULTS.enabled());
        assertTrue(DEFAULTS.activeWithLastSurvivor());
        assertEquals(48, DEFAULTS.sight().range());
        assertEquals(0.25D, DEFAULTS.stalk().threshold());
        assertEquals(0.6D, DEFAULTS.hunt().threshold());
        assertEquals(20, DEFAULTS.stalk().minDistance());
        assertEquals(35, DEFAULTS.stalk().maxDistance());
        assertEquals(40, DEFAULTS.stalk().minAngle());
        assertEquals(70, DEFAULTS.stalk().maxAngle());
        assertEquals(10, DEFAULTS.hunt().minStalkSeconds());
        assertEquals(45, DEFAULTS.hunt().cooldownSeconds());
        assertEquals(1.5D, DEFAULTS.hunt().catchDistance());
        assertEquals(15, DEFAULTS.personalSpace());
        assertEquals(2, DEFAULTS.catching().betrayalCatchCount());
        assertEquals(0.15D, DEFAULTS.catching().betrayalChance());
        assertEquals(3.0D, DEFAULTS.routeLinkDistance());
        assertEquals(0.15D, DEFAULTS.wander().stopChance());
        assertEquals(1500, DEFAULTS.wander().stopMinMillis());
        assertEquals(4000, DEFAULTS.wander().stopMaxMillis());
    }

    @Test
    @DisplayName("Reading without keys gives exactly the defaults")
    void readWithoutKeysGivesDefaults() {
        assertEquals(DEFAULTS, CreekConfig.read(ConfigSection.root(new java.util.Properties()).section("creek.")));
    }

    @Test
    @DisplayName("The link distance has to be above 0")
    void linkDistanceAboveZero() {
        assertRejected("routeLinkDistance",
                () -> with(0.0D, DEFAULTS.personalSpace(), DEFAULTS.sight(), DEFAULTS.stalk(), DEFAULTS.hunt()));
    }

    @Test
    @DisplayName("The stalk threshold has to stay below the hunt threshold")
    void stalkThresholdBelowHunt() {
        assertRejected("huntThreshold",
                () -> with(3.0D, 15, DEFAULTS.sight(), stalk(0.6D, 20, 40), hunt(0.6D)));
    }

    @Test
    @DisplayName("A hiding place inside the cone is rejected")
    void hidingPlaceOutsideTheCone() {
        assertRejected("stalkMinAngle",
                () -> with(3.0D, 15, new CreekConfig.Sight(48, 40), stalk(0.25D, 20, 40), DEFAULTS.hunt()));
    }

    @Test
    @DisplayName("The personal space has to fit inside the stalk band")
    void personalSpaceInsideTheBand() {
        assertRejected("stalkMinDistance",
                () -> with(3.0D, 20, DEFAULTS.sight(), stalk(0.25D, 20, 40), DEFAULTS.hunt()));
    }

    @Test
    @DisplayName("The stalk distance range must not run backwards")
    void stalkDistanceForwards() {
        assertRejected("stalkMinDistance", () -> new CreekConfig.Stalk(0.25D, 40, 35, 40, 70, 700, 45, 90));
    }

    @Test
    @DisplayName("Hunting, he is faster than a walking survivor")
    void huntIsFasterThanWalking() {
        // A walking player covers about 0.216 blocks per tick, the speed attribute is blocks per tick.
        assertTrue(DEFAULTS.hunt().speed() > 0.216D);
    }

    @Test
    @DisplayName("Random stops need a chance between 0 and 1 and a range that does not run backwards")
    void randomStopsAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> new CreekConfig.Wander(1500, 0.07D, 1.5D, 1500, 4000));
        assertThrows(IllegalArgumentException.class, () -> new CreekConfig.Wander(1500, 0.07D, 0.15D, -1, 4000));
        assertRejected("randomStopMinMillis", () -> new CreekConfig.Wander(1500, 0.07D, 0.15D, 5000, 4000));
        new CreekConfig.Wander(1500, 0.07D, 0.0D, 0, 0);
    }

    @Test
    @DisplayName("The launch height is 5 blocks by default, between 0 and 20, and 0 turns it off")
    void launchHeightIsChecked() {
        assertEquals(5.0D, DEFAULTS.catching().launchHeight());
        assertThrows(IllegalArgumentException.class, () -> catching(-0.1D, 0.5D, 4.0D));
        assertThrows(IllegalArgumentException.class, () -> catching(20.1D, 0.5D, 4.0D));
        catching(0.0D, 0.5D, 4.0D);
        catching(20.0D, 0.5D, 4.0D);
    }

    @Test
    @DisplayName("The swap chance is 0.5 by default and must lie between 0 and 1")
    void swapChanceIsChecked() {
        assertEquals(0.5D, DEFAULTS.catching().swapChance());
        assertThrows(IllegalArgumentException.class, () -> catching(5.0D, -0.1D, 4.0D));
        assertThrows(IllegalArgumentException.class, () -> catching(5.0D, 1.1D, 4.0D));
        catching(5.0D, 0.0D, 4.0D);
        catching(5.0D, 1.0D, 4.0D);
    }

    @Test
    @DisplayName("The launch damage is 4 by default, between 0 and 20, and 0 turns it off")
    void launchDamageIsChecked() {
        assertEquals(4.0D, DEFAULTS.catching().launchDamage());
        assertThrows(IllegalArgumentException.class, () -> catching(5.0D, 0.5D, -0.1D));
        assertThrows(IllegalArgumentException.class, () -> catching(5.0D, 0.5D, 20.1D));
        catching(5.0D, 0.5D, 0.0D);
        catching(5.0D, 0.5D, 20.0D);
    }
}
