package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
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
 * @param pageProximityEnabled  whether survivors hear a sound while a page is nearby
 * @param pageProximityRange    how far away a page may be and still be heard, in blocks
 * @param pageProximityInterval the number of ticks between two proximity sounds
 * @param pageProximitySound    the sound played while a page is nearby
 * @param pageProximityVolumeFactor how far past the range the chime's falloff is stretched
 * @param damageSoundEnabled     whether a player hears a sound when they take damage
 * @param damageSoundCooldown    the number of ticks before the damage sound is played again
 * @param damageSound            the sound played to a player who was just hit
 * @param glitchRange           how close the slender has to be before the sight of him tears a
 *                              survivor's view, in blocks
 * @param glitchCloseRange      the distance in blocks at which the tearing is at its worst
 * @param glitchViewAngle       how far off the centre of their view he may stand and still count
 *                              as seen, in degrees
 * @param lobbyAtmosphereShare  how far the lobby's atmosphere is taken towards the map's own
 * @param slenderStaticEnabled  whether the slender hears static as his pages are collected
 * @param slenderStaticSound    the sound the static is built from
 * @param slenderStaticQuietInterval   the seconds between two bursts while no page has been found
 * @param slenderStaticFranticInterval the seconds between two bursts once every page is gone
 * @param slenderStaticMinVolume       how loud the static is while no page has been found
 * @param slenderStaticMaxVolume       how loud the static is once every page is gone
 * @author theEvilReaper
 * @version 1.4.0
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
        @Nullable String resourcePackSha1,
        boolean pageProximityEnabled,
        int pageProximityRange,
        int pageProximityInterval,
        Key pageProximitySound,
        float pageProximityVolumeFactor,
        boolean damageSoundEnabled,
        int damageSoundCooldown,
        Key damageSound,
        int glitchRange,
        int glitchCloseRange,
        int glitchViewAngle,
        boolean slenderStaticEnabled,
        Key slenderStaticSound,
        int slenderStaticQuietInterval,
        int slenderStaticFranticInterval,
        float slenderStaticMinVolume,
        float slenderStaticMaxVolume,
        float lobbyAtmosphereShare
) implements GameConfig {

}
