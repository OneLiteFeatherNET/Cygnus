package net.onelitefeather.cygnus.common.config;

/**
 * Represents the configuration settings for a game.
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record GameConfig(int slenderTeamSize, int survivorTeamSize) {
    /**
     * The minimum time in seconds before Slender can re-check a player's location.
     */
    public static final int MINIMUM_SLENDER_RE_CHECK = 120;

    /**
     * The minimum number of players required to start a game.
     */
    public static final int MIN_PLAYERS = 1;
    /**
     * The maximum number of times a player on the Slender team can be revived.
     */
    public static final int MAX_REVIVE_COUNT_SLENDER = MIN_PLAYERS - 1;

    /**
     * The maximum duration of a game in minutes.
     */
    public static int MAX_GAME_TIME = 900;

    public static final int FORCE_START_TIME = 11;

    public static final int LOBBY_PHASE_TIME = 30;

    /**
     * The maximum number of players allowed in the game, calculated as the sum of
     * slenderTeamSize and survivorTeamSize.
     */
    public static final int MAX_PLAYERS = 13;

}
