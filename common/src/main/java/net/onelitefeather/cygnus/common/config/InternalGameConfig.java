package net.onelitefeather.cygnus.common.config;

/**
 * The {@link InternalGameConfig} is the fallback configuration if no other configuration is available.
 * It provides default values for the game configuration.
 * These values should be only modified if the case is necessary and the default values are not suitable.
 *
 * @param minPlayers       the minimum number of players required to start a game
 * @param maxPlayers       the maximum number of players allowed in the game
 * @param lobbyTime        the time in seconds before the game starts
 * @param gameTime      the maximum duration of a game in minutes
 * @param slenderTeamSize  the size of the slender team
 * @param survivorTeamSize the size of the survivor team
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
record InternalGameConfig(
        int minPlayers,
        int maxPlayers,
        int lobbyTime,
        int gameTime,
        int slenderTeamSize,
        int survivorTeamSize
) implements GameConfig {

    private static final GameConfig DEFAULT = new InternalGameConfig(2, 13, 30, 900, 1, 12);

    /**
     * Returns the default configuration for the game.
     *
     * @return the default configuration
     */
    public static GameConfig defaultConfig() {
        return DEFAULT;
    }
}


