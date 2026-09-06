package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The {@link GameConfig} interface represents the structure for a configuration which is used by the game.
 * It contains some values which can be adjusted to change specific settings for the game.
 * There are also some static values in the interface which are also used in the game.
 * Each static value indicates that it is a constant value and should not be changed.
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 */
public sealed interface GameConfig permits GameConfigImpl, InternalGameConfig {

    /* The name of the Slender team.
     */
    String SLENDER_TEAM_NAME = "Slender";

    Key SLENDER_KEY = Key.key("cygnus", "slender");
    /**
     * The name of the Survivor team.
     */
    String SURVIVOR_TEAM_NAME = "Survivor";

    Key SURVIVOR_KEY = Key.key("cygnus", "survivor");

    String SPECTATOR_TEAM_NAME = "Spectator";

    Key SPECTATOR_KEY = Key.key("cygnus", "spectator");

    int MIN_ACTIVE_PAGE_COUNT = 4;

    int PAGE_TTL_TIME = 60;

    int FORCE_START_TIME = 11;

    int MIN_PAGE_COUNT = 8;

    /**
     * Creates a new {@link Builder} which can be used to create a new game configuration.
     *
     * @return the builder instance
     */
    @Contract(pure = true)
    static Builder builder() {
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
     * Returns the DSN which points Sentry at the project the errors of this service belong to.
     * <p>
     * The Sentry integration is opt-in: without a DSN there is nothing to report to, so no client
     * is set up at all. That is the expected state for local runs and tests.
     * </p>
     *
     * @return the configured DSN, or {@code null} when the Sentry integration stays off
     * @since 2.11.0
     */
    @Nullable
    String sentryDsn();

    /**
     * Returns the location the client downloads the ResourcePack from.
     *
     * @return the configured URL, or {@code null} when the ResourcePack feature stays off
     * @since 2.11.0
     */
    @Nullable
    URI resourcePackUrl();

    /**
     * Returns the SHA-1 checksum the client verifies the downloaded ResourcePack against.
     * <p>
     * A production setup always states the checksum: it is what lets a client reuse the pack it
     * already has instead of downloading it again on every join. Leaving it out is a test-only
     * convenience - the checksum is then computed from the pack behind {@link #resourcePackUrl()}
     * at runtime.
     * </p>
     *
     * @return the configured checksum, or {@code null} when it has to be computed
     * @since 2.11.0
     */
    @Nullable
    String resourcePackSha1();

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
        Builder minPlayers(int minPlayers);

        /**
         * Sets the maximum number of players allowed in the game.
         *
         * @param maxPlayers the maximum number of players
         * @return the builder instance
         */
        Builder maxPlayers(int maxPlayers);

        /**
         * Sets the lobby time in seconds.
         *
         * @param lobbyTime the lobby time
         * @return the builder instance
         * @throws IllegalArgumentException if the lobby time is than the {@link GameConfig#FORCE_START_TIME}
         */
        Builder lobbyTime(int lobbyTime);

        /**
         * Sets the maximum game time in seconds.
         *
         * @param gameTime the maximum game time
         * @return the builder instance
         */
        Builder gameTime(int gameTime);

        /**
         * Sets the size of the slender team.
         *
         * @param slenderTeamSize the size of the slender team
         * @return the builder instance
         * @throws IllegalArgumentException if the slender team size is smaller than 1
         */
        Builder slenderTeamSize(int slenderTeamSize);

        /**
         * Sets the size of the survivor team.
         *
         * @param survivorTeamSize the size of the survivor team
         * @return the builder instance
         * @throws IllegalArgumentException if the survivor team size is smaller than 1
         */
        Builder survivorTeamSize(int survivorTeamSize);

        /**
         * Sets the DSN which points Sentry at the project the errors of this service belong to.
         *
         * @param sentryDsn the DSN, or {@code null} to leave the Sentry integration off
         * @return the builder instance
         * @since 2.11.0
         */
        Builder sentryDsn(@Nullable String sentryDsn);

        /**
         * Sets the location the client downloads the ResourcePack from.
         *
         * @param resourcePackUrl the URL, or {@code null} to leave the ResourcePack feature off
         * @return the builder instance
         * @since 2.11.0
         */
        Builder resourcePackUrl(@Nullable URI resourcePackUrl);

        /**
         * Sets the SHA-1 checksum the client verifies the downloaded ResourcePack against.
         *
         * @param resourcePackSha1 the checksum, or {@code null} to have it computed at runtime
         * @return the builder instance
         * @since 2.11.0
         */
        Builder resourcePackSha1(@Nullable String resourcePackSha1);

        /**
         * Builds the game configuration.
         *
         * @return the created configuration
         */
        GameConfig build();
    }
}
