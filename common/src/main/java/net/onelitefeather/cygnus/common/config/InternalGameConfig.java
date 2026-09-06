package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The {@link InternalGameConfig} is the fallback configuration if no other configuration is available.
 * It provides default values for the game configuration.
 * These values should be only modified if the case is necessary and the default values are not suitable.
 *
 * @param minPlayers        the minimum number of players required to start a game
 * @param maxPlayers        the maximum number of players allowed in the game
 * @param lobbyTime         the time in seconds before the game starts
 * @param gameTime          the maximum duration of a game in seconds
 * @param slenderTeamSize   the size of the slender team
 * @param survivorTeamSize  the size of the survivor team
 * @param sentryDsn         the DSN to report errors to, or {@code null} to keep Sentry off
 * @param resourcePackUrl   the location the client downloads the ResourcePack from, or {@code null}
 *                          to keep the ResourcePack feature off
 * @param resourcePackSha1  the checksum of the ResourcePack, or {@code null} to have it computed
 * @param pageProximityEnabled  whether survivors hear a sound while a page is nearby
 * @param pageProximityRange    how far away a page may be and still be heard, in blocks
 * @param pageProximityInterval the number of ticks between two proximity sounds
 * @param pageProximitySound    the sound played while a page is nearby
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
record InternalGameConfig(
        int minPlayers,
        int maxPlayers,
        int lobbyTime,
        int gameTime,
        int slenderTeamSize,
        int survivorTeamSize,
        @Nullable String sentryDsn,
        @Nullable URI resourcePackUrl,
        @Nullable String resourcePackSha1,
        boolean pageProximityEnabled,
        int pageProximityRange,
        int pageProximityInterval,
        Key pageProximitySound
) implements GameConfig {

    // Sentry and the ResourcePack are opt-in: a service that says nothing about them reports to
    // nothing and pushes nothing, which is what a local run without a config file needs.
    // The proximity hint is on by default: a page that cannot be heard at all is the pre-2.12
    // behaviour, and a map is easier to play with the hint than without it.
    private static final GameConfig DEFAULT = new InternalGameConfig(
            2, 13, 30, 900, 1, 12, null, null, null,
            true, 20, 20, GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND);

    /**
     * Returns the default configuration for the game.
     *
     * @return the default configuration
     */
    public static GameConfig defaultConfig() {
        return DEFAULT;
    }
}
