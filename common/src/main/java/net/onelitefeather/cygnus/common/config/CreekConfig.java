package net.onelitefeather.cygnus.common.config;

/**
 * Tunes the creek, the figure that walks the map next to the slender.
 * <p>
 * Every number his behavior depends on lives here, so distances and timings can be tuned in a
 * playtest without touching the code. The compact constructor rejects combinations that would
 * break the behavior outright rather than merely make it feel off. a stalk band that runs
 * backwards, or a hiding place inside the cone that counts as being seen the moment he gets there.
 * </p>
 *
 * @param enabled                whether the creek takes part in a round at all
 * @param activeWithLastSurvivor whether he keeps going once only one survivor is left
 * @param sightRange             how far away a survivor can notice him, in blocks
 * @param sightViewAngle         how far off the center of a survivor's view he may stand and still
 *                               count as seen, in degrees
 * @param wanderPauseMillis      how long he stands still when someone spots him while wandering
 * @param wanderSpeed            his movement speed while wandering, in blocks per tick
 * @param huntSpeed              his movement speed while hunting, in blocks per tick; a walking
 *                               survivor covers about 0.216, so this has to stay above that
 * @param stalkThreshold         the dread at which he picks a survivor to stalk
 * @param huntThreshold          the dread at which a stalk turns into a hunt
 * @param stalkMinDistance       the closest he stands to the survivor he stalks, in blocks
 * @param stalkMaxDistance       the farthest he stands from the survivor he stalks, in blocks
 * @param stalkMinAngle          the smallest angle off the survivor's view he places himself at
 * @param stalkMaxAngle          the largest angle off the survivor's view he places himself at
 * @param stalkRevealMillis      how long he lets himself be seen before he moves on
 * @param stalkMinSeconds        the shortest a stalk lasts
 * @param stalkMaxSeconds        the longest a stalk lasts
 * @param huntMaxSeconds         the longest a hunt lasts before he gives up
 * @param catchDistance          how close he has to come to catch a survivor, in blocks
 * @param vanishMinSeconds       the shortest he stays away, reached at full dread
 * @param vanishMaxSeconds       the longest he stays away, reached at no dread
 * @param respawnMinDistance     how far from every survivor he reappears, in blocks
 * @param personalSpace          how close he lets a survivor come outside a hunt, in blocks
 * @param stuckMillis            how long he may make no progress before he takes a shortcut
 * @param dreadPageWeight        the share of the dread that comes from the pages found
 * @param dreadTimeWeight        the share of the dread that comes from the elapsed round time
 * @param dreadIsolationWeight   the share of the dread that comes from being alone
 * @param isolationRadius        how far the nearest other survivor has to be to count as alone
 * @param betrayalCatchCount     from which catch on he always gives a survivor away
 * @param betrayalChance         the chance he gives a survivor away before that
 * @param betrayalGlowSeconds    how long the slender sees a betrayed survivor glow
 * @param slownessSeconds        how long a caught survivor is slowed
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
        int slownessSeconds
) {

    /**
     * The values from the design: on, and tuned for a round of about fifteen minutes.
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
            2, 0.15D, 6, 4
    );

    /**
     * Rejects values that would break the creek's behavior.
     *
     * @throws IllegalArgumentException if a value is out of range or two values do not fit together
     */
    public CreekConfig {
        atLeast("sightRange", sightRange, 1);
        between("sightViewAngle", sightViewAngle, 1, GameConfig.MAX_GLITCH_VIEW_ANGLE);
        atLeast("wanderPauseMillis", wanderPauseMillis, 0);
        positive("wanderSpeed", wanderSpeed);
        positive("huntSpeed", huntSpeed);
        positive("stalkThreshold", stalkThreshold);
        between("huntThreshold", huntThreshold, 0.0D, 1.0D);
        below("stalkThreshold", stalkThreshold, "huntThreshold", huntThreshold);
        atLeast("personalSpace", personalSpace, 1);
        below("personalSpace", personalSpace, "stalkMinDistance", stalkMinDistance);
        below("stalkMinDistance", stalkMinDistance, "stalkMaxDistance", stalkMaxDistance);
        // A hiding place inside the cone would count as being seen the moment he got there.
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
