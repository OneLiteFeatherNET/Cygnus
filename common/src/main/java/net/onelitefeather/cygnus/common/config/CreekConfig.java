package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the creek, the second figure next to the slender.
 * <p>
 * All numbers that control the creek's behavior live here, so they can be tuned in a playtest
 * without changing code. The constructor rejects combinations that would break the behavior,
 * for example a stalk distance range that runs backwards, or a hiding spot inside the view cone.
 * </p>
 *
 * @param enabled                whether the creek takes part in a round
 * @param activeWithLastSurvivor whether the creek keeps going when only one survivor is left
 * @param sightRange             how far away a survivor can notice the creek, in blocks
 * @param sightViewAngle         the maximum angle from a survivor's view center at which the
 *                               creek counts as seen, in degrees
 * @param wanderPauseMillis      how long the creek stops when spotted while wandering
 * @param wanderSpeed            movement speed while wandering, in blocks per tick
 * @param huntSpeed              movement speed while hunting, in blocks per tick; must be above
 *                               a walking survivor's 0.216
 * @param stalkThreshold         the dread at which the creek starts stalking a survivor
 * @param huntThreshold          the dread at which a stalk turns into a hunt
 * @param stalkMinDistance       minimum distance to the stalked survivor, in blocks
 * @param stalkMaxDistance       maximum distance to the stalked survivor, in blocks
 * @param stalkMinAngle          minimum angle from the stalked survivor's view direction, in degrees
 * @param stalkMaxAngle          maximum angle from the stalked survivor's view direction, in degrees
 * @param stalkRevealMillis      how long the creek stays visible before it teleports away
 * @param stalkMinSeconds        minimum length of a stalk
 * @param stalkMaxSeconds        maximum length of a stalk
 * @param huntMaxSeconds         maximum length of a hunt
 * @param catchDistance          distance at which the creek catches a survivor, in blocks
 * @param vanishMinSeconds       shortest vanish time, used at full dread
 * @param vanishMaxSeconds       longest vanish time, used at no dread
 * @param respawnMinDistance     minimum distance to every survivor when reappearing, in blocks
 * @param personalSpace          how close a survivor may come outside a hunt, in blocks
 * @param stuckMillis            how long the creek may make no progress before taking a shortcut
 * @param dreadPageWeight        dread share from found pages
 * @param dreadTimeWeight        dread share from elapsed round time
 * @param dreadIsolationWeight   dread share from being alone
 * @param isolationRadius        distance to the nearest survivor at which someone counts as alone
 * @param betrayalCatchCount     from this catch on, a survivor is always revealed to the slender
 * @param betrayalChance         chance of a reveal on earlier catches
 * @param betrayalGlowSeconds    how long a revealed survivor glows for the slender
 * @param slownessSeconds        how long a caught survivor is slowed
 * @param routeLinkDistance      how close two route ends have to be to count as linked, in blocks
 * @param randomStopChance       chance that the creek stops for a moment after reaching a waypoint
 * @param randomStopMinMillis    shortest random stop, in milliseconds
 * @param randomStopMaxMillis    longest random stop, in milliseconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record CreekConfig(
        boolean enabled,
        boolean activeWithLastSurvivor,
        int sightRange,
        int sightViewAngle,
        int wanderPauseMillis,
        double wanderSpeed,
        double huntSpeed,
        double stalkThreshold,
        double huntThreshold,
        int stalkMinDistance,
        int stalkMaxDistance,
        int stalkMinAngle,
        int stalkMaxAngle,
        int stalkRevealMillis,
        int stalkMinSeconds,
        int stalkMaxSeconds,
        int huntMaxSeconds,
        double catchDistance,
        int vanishMinSeconds,
        int vanishMaxSeconds,
        int respawnMinDistance,
        int personalSpace,
        int stuckMillis,
        double dreadPageWeight,
        double dreadTimeWeight,
        double dreadIsolationWeight,
        int isolationRadius,
        int betrayalCatchCount,
        double betrayalChance,
        int betrayalGlowSeconds,
        int slownessSeconds,
        double routeLinkDistance,
        double randomStopChance,
        int randomStopMinMillis,
        int randomStopMaxMillis
) {

    /**
     * The default settings: enabled, and tuned for a round of about fifteen minutes.
     */
    public static final CreekConfig DEFAULT = new CreekConfig(
            true, true,
            48, 35,
            1500, 0.07D, 0.25D,
            0.25D, 0.6D,
            20, 35, 40, 70, 700, 45, 90,
            30, 1.5D,
            20, 40, 30, 15, 3000,
            0.6D, 0.3D, 0.1D, 25,
            2, 0.15D, 6, 4,
            3.0D,
            0.15D, 1500, 4000
    );

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public CreekConfig {
        atLeast("sightRange", sightRange, 1);
        between("sightViewAngle", sightViewAngle, 1, GameConfig.Glitch.MAX_VIEW_ANGLE);
        atLeast("wanderPauseMillis", wanderPauseMillis, 0);
        positive("wanderSpeed", wanderSpeed);
        positive("huntSpeed", huntSpeed);
        positive("stalkThreshold", stalkThreshold);
        between("huntThreshold", huntThreshold, 0.0D, 1.0D);
        below("stalkThreshold", stalkThreshold, "huntThreshold", huntThreshold);
        atLeast("personalSpace", personalSpace, 1);
        below("personalSpace", personalSpace, "stalkMinDistance", stalkMinDistance);
        below("stalkMinDistance", stalkMinDistance, "stalkMaxDistance", stalkMaxDistance);
        // A hiding spot inside the view cone would count as seen right away.
        below("sightViewAngle", sightViewAngle, "stalkMinAngle", stalkMinAngle);
        below("stalkMinAngle", stalkMinAngle, "stalkMaxAngle", stalkMaxAngle);
        between("stalkMaxAngle", stalkMaxAngle, 1, 180);
        atLeast("stalkRevealMillis", stalkRevealMillis, 0);
        atLeast("stalkMinSeconds", stalkMinSeconds, 1);
        notAbove("stalkMinSeconds", stalkMinSeconds, "stalkMaxSeconds", stalkMaxSeconds);
        atLeast("huntMaxSeconds", huntMaxSeconds, 1);
        positive("catchDistance", catchDistance);
        atLeast("vanishMinSeconds", vanishMinSeconds, 0);
        notAbove("vanishMinSeconds", vanishMinSeconds, "vanishMaxSeconds", vanishMaxSeconds);
        atLeast("respawnMinDistance", respawnMinDistance, 1);
        atLeast("stuckMillis", stuckMillis, 1);
        atLeast("dreadPageWeight", dreadPageWeight, 0.0D);
        atLeast("dreadTimeWeight", dreadTimeWeight, 0.0D);
        atLeast("dreadIsolationWeight", dreadIsolationWeight, 0.0D);
        atLeast("isolationRadius", isolationRadius, 1);
        atLeast("betrayalCatchCount", betrayalCatchCount, 1);
        between("betrayalChance", betrayalChance, 0.0D, 1.0D);
        atLeast("betrayalGlowSeconds", betrayalGlowSeconds, 1);
        atLeast("slownessSeconds", slownessSeconds, 0);
        positive("routeLinkDistance", routeLinkDistance);
        between("randomStopChance", randomStopChance, 0.0D, 1.0D);
        atLeast("randomStopMinMillis", randomStopMinMillis, 0);
        notAbove("randomStopMinMillis", randomStopMinMillis, "randomStopMaxMillis", randomStopMaxMillis);
    }

    private static void atLeast(String name, double value, double minimum) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " (" + value + ") must be at least " + minimum);
        }
    }

    private static void positive(String name, double value) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " (" + value + ") must be above 0");
        }
    }

    private static void between(String name, double value, double minimum, double maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " (" + value + ") must be between " + minimum + " and " + maximum);
        }
    }

    private static void below(String lowerName, double lower, String upperName, double upper) {
        if (lower >= upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + lower + ") must be below " + upperName + " (" + upper + ")");
        }
    }

    private static void notAbove(String lowerName, double lower, String upperName, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + lower + ") must not be above " + upperName + " (" + upper + ")");
        }
    }
}
