package net.onelitefeather.cygnus.stats;

/**
 * Why a round ended, as the statistics see it. The constants mirror the game's finish reasons by name
 * and are stored by name, so a renamed constant needs a migration.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public enum FinishReason {

    /** The round timer ran out. */
    TIME_OVER,
    /** The survivors found every page. */
    ALL_PAGES_FOUND,
    /** Every survivor is dead. */
    ALL_SURVIVOR_DEAD,
    /** The Slender left the game. */
    SLENDER_LEFT,
    /** A survivor left and too few remain. */
    SURVIVOR_LEFT
}
