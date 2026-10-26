package net.onelitefeather.cygnus.stats;

/**
 * Why a slender pass balance changed. The names are stored as they are, the ledger checks them.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public enum LedgerReason {
    /** An administrator added passes. */
    ADMIN_GIVE,
    /** An administrator removed passes. */
    ADMIN_TAKE,
    /** An administrator set the balance. */
    ADMIN_SET,
    /** A pass was spent for a round in which the player played the Slender. */
    REDEEM
}
