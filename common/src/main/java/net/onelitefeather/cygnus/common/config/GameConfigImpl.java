package net.onelitefeather.cygnus.common.config;

import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The {@link GameConfigImpl} is the implementation of the {@link GameConfig} interface.
 * It represents a configuration which is used to adjust some settings for the game.
 * The configuration is immutable and can't be changed after the creation.
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
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public record GameConfigImpl(
        int minPlayers,
        int maxPlayers,
        int lobbyTime,
        int gameTime,
        int slenderTeamSize,
        int survivorTeamSize,
        @Nullable String sentryDsn,
        @Nullable URI resourcePackUrl,
        @Nullable String resourcePackSha1
) implements GameConfig {

}
