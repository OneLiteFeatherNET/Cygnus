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

    // Sentry and the ResourcePack are opt-in: a service that says nothing about them reports to
    // nothing and pushes nothing, which is what a local run without a config file needs.
    // The proximity hint is on by default: a page that cannot be heard at all is the pre-2.12
    // behaviour, and a map is easier to play with the hint than without it.
    // The damage feedback is on by default for the same reason: taking a hit in silence is a bug,
    // not a setting. The cooldown of 20 ticks lets through every second damage tick of a draining
    // slender, which is enough to notice and not enough to grate.
    // The slender's static is on by default too: it is the only thing that tells him how far the
    // survivors have got without putting the page counter in front of him.
    private static final GameConfig DEFAULT = new InternalGameConfig(
            2, 13, 30, 900, 1, 12, null, null, null,
            true, 20, 20, GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND,
            GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR,
            true, 20, GameConfig.DEFAULT_DAMAGE_SOUND,
            GameConfig.DEFAULT_GLITCH_RANGE,
            GameConfig.DEFAULT_GLITCH_CLOSE_RANGE,
            GameConfig.DEFAULT_GLITCH_VIEW_ANGLE,
            true, GameConfig.DEFAULT_SLENDER_STATIC_SOUND,
            GameConfig.DEFAULT_SLENDER_STATIC_QUIET_INTERVAL,
            GameConfig.DEFAULT_SLENDER_STATIC_FRANTIC_INTERVAL,
            GameConfig.DEFAULT_SLENDER_STATIC_MIN_VOLUME,
            GameConfig.DEFAULT_SLENDER_STATIC_MAX_VOLUME,
            GameConfig.DEFAULT_LOBBY_ATMOSPHERE_SHARE);

    /**
     * Returns the default configuration for the game.
     *
     * @return the default configuration
     */
    public static GameConfig defaultConfig() {
        return DEFAULT;
    }
}
