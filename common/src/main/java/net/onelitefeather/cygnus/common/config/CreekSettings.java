package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.below;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;

/**
 * The creek values a map sets for itself, read from its {@code creek.json}.
 * <p>
 * They are the distances that have to fit the map: how far the creek is seen, where it stands
 * while stalking, how far its routes reach to each other and how high a catch throws a survivor
 * before the ceiling gets in the way. A value the file leaves out takes its default.
 * </p>
 *
 * @param sightRange         how far away a survivor can notice the creek, in blocks
 * @param stalkMinDistance   minimum distance to the stalked survivor, in blocks
 * @param stalkMaxDistance   maximum distance to the stalked survivor, in blocks
 * @param respawnMinDistance minimum distance to every survivor when reappearing, in blocks
 * @param routeLinkDistance  how close two route ends have to be to count as linked, in blocks
 * @param personalSpace      how close a survivor may come outside a hunt, in blocks
 * @param launchHeight       how high a caught survivor is thrown, in blocks. 0 turns the launch off
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public record CreekSettings(
        int sightRange,
        int stalkMinDistance,
        int stalkMaxDistance,
        int respawnMinDistance,
        double routeLinkDistance,
        int personalSpace,
        double launchHeight
) {

    /** Twice the default; past that the creek is noticed from the far end of most maps. */
    public static final int MAX_SIGHT_RANGE = 96;

    /** Past this the creek stalks from so far away that nobody notices it. */
    public static final int MAX_STALK_DISTANCE = 64;

    /** Past this a small map has no spot left to come back at. */
    public static final int MAX_RESPAWN_MIN_DISTANCE = 128;

    /** Below this two separate routes would only link where they touch. */
    public static final double MIN_ROUTE_LINK_DISTANCE = 0.5D;

    /** Past this routes link across walls. */
    public static final double MAX_ROUTE_LINK_DISTANCE = 16.0D;

    /** Past this the creek keeps so far away that a stalk never gets close. */
    public static final int MAX_PERSONAL_SPACE = 32;

    /** The highest launch a catch may be set to, in blocks. */
    public static final double MAX_LAUNCH_HEIGHT = 20.0D;

    /** The values every map used before maps had settings of their own. */
    public static final CreekSettings DEFAULT = new CreekSettings(48, 20, 35, 30, 3.0D, 15, 5.0D);

    /**
     * Checks every value against its bounds and the stalk band against the personal space.
     *
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public CreekSettings {
        between("sightRange", sightRange, 1, MAX_SIGHT_RANGE);
        between("stalkMinDistance", stalkMinDistance, 1, MAX_STALK_DISTANCE);
        between("stalkMaxDistance", stalkMaxDistance, 1, MAX_STALK_DISTANCE);
        between("respawnMinDistance", respawnMinDistance, 1, MAX_RESPAWN_MIN_DISTANCE);
        between("routeLinkDistance", routeLinkDistance, MIN_ROUTE_LINK_DISTANCE, MAX_ROUTE_LINK_DISTANCE);
        between("personalSpace", personalSpace, 1, MAX_PERSONAL_SPACE);
        between("launchHeight", launchHeight, 0.0D, MAX_LAUNCH_HEIGHT);
        // Outside a hunt the creek keeps its distance, so a stalk spot closer than that is never used.
        below("personalSpace", personalSpace, "stalkMinDistance", stalkMinDistance);
        below("stalkMinDistance", stalkMinDistance, "stalkMaxDistance", stalkMaxDistance);
    }
}
