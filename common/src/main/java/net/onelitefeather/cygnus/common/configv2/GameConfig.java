package net.onelitefeather.cygnus.common.configv2;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link GameConfig} interface represents the structure for a configuration which is used by the game.
 * It contains some values which can be adjusted to change specific settings for the game.
 * There are also some static values in the interface which are also used in the game.
 * Each static value indicates that it is a constant value and should not be changed.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public sealed interface GameConfig permits GameConfigImpl, InternalGameConfig {

    /* The name of the Slender team.
     */
    String SLENDER_TEAM_NAME = "Slender";
    /**
     * The name of the Survivor team.
     */
    String SURVIVOR_TEAM_NAME = "Survivor";

    String MAP_FILE_NAME = "map.json";

    int MIN_PAGE_COUNT = 4;

    int PAGE_TTL_TIME = 60;

    int FORCE_START_TIME = 11;

    /**
     * Creates a new {@link Builder} which can be used to create a new game configuration.
     *
     * @return the builder instance
     */
    @Contract(pure = true)
    static @NotNull Builder builder() {
        return new GameConfigBuilder();
    }

    /**
     * Returns the minimum number of players required to start a game.
     *
     * @return the minimum number of players
     */
    int minPlayers();

    /**
     * Returns the maximum number of players allowed in the game.
     *
     * @return the maximum number of players
     */
    int maxPlayers();

    /**
     * Returns the lobby time in seconds.
     *
     * @return the lobby time
     */
    int lobbyTime();

    /**
     * Returns the maximum game time in seconds.
     *
     * @return the maximum game time
     */
    int gameTime();

    /**
     * Returns the size of the slender team.
     *
     * @return the size of the slender team
     */
    int slenderTeamSize();

    /**
     * Returns the size of the survivor team.
     *
     * @return the size of the survivor team
     */
    int survivorTeamSize();

    /**
     * The {@link Builder} interface is used to create a new game configuration.
     * It provides methods to set the values for the configuration.
     *
     * @author theEvilReaper
     * @version 1.0.0
     * @since 1.0.0
     */
    sealed interface Builder permits GameConfigBuilder {

        /**
         * Sets the minimum number of players required to start a game.
         *
         * @param minPlayers the minimum number of players
         * @return the builder instance
         */
        @NotNull Builder minPlayers(int minPlayers);

        /**
         * Sets the maximum number of players allowed in the game.
         *
         * @param maxPlayers the maximum number of players
         * @return the builder instance
         */
        @NotNull Builder maxPlayers(int maxPlayers);

        /**
         * Sets the lobby time in seconds.
         *
         * @param lobbyTime the lobby time
         * @return the builder instance
         * @throws IllegalArgumentException if the lobby time is than the {@link GameConfig#FORCE_START_TIME}
         */
        @NotNull Builder lobbyTime(int lobbyTime);

        /**
         * Sets the maximum game time in seconds.
         *
         * @param gameTime the maximum game time
         * @return the builder instance
         */
        @NotNull Builder gameTime(int gameTime);

        /**
         * Sets the size of the slender team.
         *
         * @param slenderTeamSize the size of the slender team
         * @return the builder instance
         * @throws IllegalArgumentException if the slender team size is smaller than 1
         */
        @NotNull Builder slenderTeamSize(int slenderTeamSize);

        /**
         * Sets the size of the survivor team.
         *
         * @param survivorTeamSize the size of the survivor team
         * @return the builder instance
         * @throws IllegalArgumentException if the survivor team size is smaller than 1
         */
        @NotNull Builder survivorTeamSize(int survivorTeamSize);

        /**
         * Builds the game configuration.
         *
         * @return the created configuration
         */
        @NotNull GameConfig build();
    }

}
