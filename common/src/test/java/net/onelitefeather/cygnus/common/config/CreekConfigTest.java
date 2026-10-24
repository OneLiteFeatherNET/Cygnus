package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.onelitefeather.cygnus.common.config.ConfigAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekConfigTest {

    private static final CreekConfig DEFAULTS = CreekConfig.DEFAULT;

    /** The defaults with the given stalk, sight and hunt groups. */
    private static CreekConfig with(CreekConfig.Sight sight, CreekConfig.Stalk stalk, CreekConfig.Hunt hunt) {
        return new CreekConfig(DEFAULTS.enabled(), DEFAULTS.activeWithLastSurvivor(), DEFAULTS.stuckMillis(), sight,
                DEFAULTS.wander(), stalk, hunt, DEFAULTS.vanish(), DEFAULTS.catching());
    }

    /** The default stalk group with another threshold and minimum angle. */
    private static CreekConfig.Stalk stalk(double threshold, int minAngle) {
        CreekConfig.Stalk stalk = DEFAULTS.stalk();
        return new CreekConfig.Stalk(threshold, minAngle, stalk.maxAngle(), stalk.revealMillis(), stalk.minSeconds(),
                stalk.maxSeconds());
    }

    /** The default hunt group with another threshold. */
    private static CreekConfig.Hunt hunt(double threshold) {
        CreekConfig.Hunt hunt = DEFAULTS.hunt();
        return new CreekConfig.Hunt(hunt.speed(), threshold, hunt.maxSeconds(), hunt.minStalkSeconds(),
                hunt.cooldownSeconds(), hunt.catchDistance());
    }


    /** The default catch group with another swap chance and launch damage. */
    private static CreekConfig.Catch catching(double swapChance, double launchDamage) {
        CreekConfig.Catch catching = DEFAULTS.catching();
        return new CreekConfig.Catch(catching.betrayalCatchCount(), catching.betrayalChance(),
                catching.betrayalGlowSeconds(), catching.slownessSeconds(), swapChance, launchDamage);
    }

    @Test
    @DisplayName("The defaults are the values from the design")
    void defaultsMatchTheDesign() {
        assertTrue(DEFAULTS.enabled());
        assertTrue(DEFAULTS.activeWithLastSurvivor());
        assertEquals(0.25D, DEFAULTS.stalk().threshold());
        assertEquals(0.6D, DEFAULTS.hunt().threshold());
        assertEquals(80, DEFAULTS.stalk().minAngle());
        assertEquals(130, DEFAULTS.stalk().maxAngle());
        assertEquals(10, DEFAULTS.hunt().minStalkSeconds());
        assertEquals(45, DEFAULTS.hunt().cooldownSeconds());
        assertEquals(1.5D, DEFAULTS.hunt().catchDistance());
        assertEquals(2, DEFAULTS.catching().betrayalCatchCount());
        assertEquals(0.15D, DEFAULTS.catching().betrayalChance());
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
    @DisplayName("The stalk threshold has to stay below the hunt threshold")
    void stalkThresholdBelowHunt() {
        assertRejected("huntThreshold",
                () -> with(DEFAULTS.sight(), stalk(0.6D, 40), hunt(0.6D)));
    }

    @Test
    @DisplayName("A hiding place inside the cone is rejected")
    void hidingPlaceOutsideTheCone() {
        assertRejected("stalkMinAngle",
                () -> with(new CreekConfig.Sight(40), stalk(0.25D, 40), DEFAULTS.hunt()));
    }


    @Test
    @DisplayName("The stalk angles have to reach past the hide angle, or no spot is ever off the screen")
    void stalkAnglesReachPastTheHideAngle() {
        assertRejected("stalkMaxAngle",
                () -> new CreekConfig.Stalk(0.25D, 40, CreekConfig.HIDE_ANGLE, 700, 45, 90));
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
    @DisplayName("The swap chance is 0.5 by default and must lie between 0 and 1")
    void swapChanceIsChecked() {
        assertEquals(0.5D, DEFAULTS.catching().swapChance());
        assertThrows(IllegalArgumentException.class, () -> catching(-0.1D, 4.0D));
        assertThrows(IllegalArgumentException.class, () -> catching(1.1D, 4.0D));
        catching(0.0D, 4.0D);
        catching(1.0D, 4.0D);
    }

    @Test
    @DisplayName("The launch damage is 4 by default, between 0 and 20, and 0 turns it off")
    void launchDamageIsChecked() {
        assertEquals(4.0D, DEFAULTS.catching().launchDamage());
        assertThrows(IllegalArgumentException.class, () -> catching(0.5D, -0.1D));
        assertThrows(IllegalArgumentException.class, () -> catching(0.5D, 20.1D));
        catching(0.5D, 0.0D);
        catching(0.5D, 20.0D);
    }
}
