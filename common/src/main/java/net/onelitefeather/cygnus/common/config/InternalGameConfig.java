package net.onelitefeather.cygnus.common.config;

import org.jetbrains.annotations.NotNull;

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

    /**
     * Returns the default configuration for the game.
     *
     * @return the default configuration
     */
    public static @NotNull GameConfig defaultConfig() {
        return Instances.INTERNAL;
    }

    /**
     * Holds the reference to the default configuration instance.
     */
    static final class Instances {
        private static final int MAX_GAME_TIME = 900;
        private static final int LOBBY_PHASE_TIME = 30;
        private static final int MAX_PLAYERS = 13;
        private static final int MIN_PLAYERS = 2;
        private static final int SLENDER_TEAM_SIZE = 1;
        private static final int SURVIVOR_TEAM_SIZE = 12;

        static final GameConfig INTERNAL;

        static {
            INTERNAL = new InternalGameConfig(
                    MIN_PLAYERS,
                    MAX_PLAYERS,
                    LOBBY_PHASE_TIME,
                    MAX_GAME_TIME,
                    SLENDER_TEAM_SIZE,
                    SURVIVOR_TEAM_SIZE
            );
        }

        private Instances() {
            throw new UnsupportedOperationException("This class cannot be instantiated");
        }
    }
}


