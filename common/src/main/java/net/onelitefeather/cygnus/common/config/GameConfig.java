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
 * @version 1.5.0
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

    int MIN_ACTIVE_PAGE_COUNT = 4 * 2;

    /**
     * How many seconds after a round starts before the first pages spawn.
     * <p>
     * Spawning immediately at {@code GameStartEvent} let survivors grab a page before they had even
     * moved from the spawn point. The delay gives them time to spread across the map first.
     * </p>
     *
     * @since 2.15.0
     */
    int PAGE_SPAWN_DELAY = 10;

    /**
     * How many seconds {@link #PAGE_SPAWN_DELAY} may randomly shift up or down, re-rolled every
     * round.
     * <p>
     * Without this the delay lands on the exact same tick every round, which players learn and
     * plan around; the jitter keeps the moment the first pages appear unpredictable.
     * </p>
     *
     * @since 2.15.0
     */
    int PAGE_SPAWN_DELAY_JITTER = 2;

    int PAGE_TTL_TIME = 60;

    int FORCE_START_TIME = 11;

    int MIN_PAGE_COUNT = 8;

    /**
     * The sound played to a survivor while a page is within {@link #pageProximityRange()}.
     * The amethyst chime is a soft, bell-less shimmer that reads as "something is here" without
     * sounding like an alarm.
     */
    Key DEFAULT_PAGE_PROXIMITY_SOUND = Key.key("block.amethyst_block.chime");

    /**
     * The largest {@link #pageProximityRange()} a configuration may ask for. Beyond this a single
     * page would be audible across a good part of the map, which stops being a hint.
     */
    int MAX_PAGE_PROXIMITY_RANGE = 64;

    /**
     * The {@link #pageProximityVolumeFactor()} a configuration gets when it says nothing.
     * <p>
     * A factor of 1 makes the chime reach exactly to {@link #pageProximityRange()} and no further,
     * which means it fades to silence precisely where the hint is supposed to start being useful.
     * Doubling that leaves roughly half the volume at the edge of the range while the service still
     * clips on the range itself.
     * </p>
     */
    float DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR = 2.0F;

    /**
     * The largest {@link #pageProximityVolumeFactor()} a configuration may ask for.
     * <p>
     * The ceiling is not about loudness - Minecraft caps a sound's amplitude at the source
     * regardless of volume - but about the falloff. The flatter it gets, the more evenly loud the
     * chime is across the whole range, until a player can no longer tell a page two blocks away
     * from one at the edge. Past 8 that distance cue is gone.
     * </p>
     */
    float MAX_PAGE_PROXIMITY_VOLUME_FACTOR = 8.0F;

    /**
     * The sound played to a player who was just hit.
     * <p>
     * The vanilla hurt sound, because that is exactly what is missing: Cygnus applies damage by
     * setting health directly, which never runs Minestom's damage pipeline and therefore never
     * plays the sound a client would otherwise hear.
     * </p>
     */
    Key DEFAULT_DAMAGE_SOUND = Key.key("entity.player.hurt");

    /**
     * The {@link #lobbyAtmosphereShare()} a configuration gets when it says nothing.
     * <p>
     * Enough of the map's own haze and colour to be recognised in the distance, far enough from it
     * that the lobby still reads as the lit room players wait in rather than as the map itself.
     * </p>
     */
    float DEFAULT_LOBBY_ATMOSPHERE_SHARE = 0.3F;

    /**
     * The static the slender hears while the survivors take his pages away.
     * <p>
     * A resource pack sound rather than a vanilla one: three 2.2 second takes of tape hiss the
     * client picks between, high-passed at 520 Hz so the effect's own pitch drop to 0.7 leaves
     * it hissing rather than humming. Nothing in vanilla comes close - rain is the nearest, and
     * it reads as weather.
     * </p>
     * <p>
     * A server running without the Cygnus pack therefore hears nothing here. That is the right
     * way round: the static is a horror cue, and half of one played through the wrong sample is
     * worse than none.
     * </p>
     */
    Key DEFAULT_SLENDER_STATIC_SOUND = Key.key("cygnus", "vhs_static");

    /** The {@link #slenderStaticQuietInterval()} a configuration gets when it says nothing. */
    int DEFAULT_SLENDER_STATIC_QUIET_INTERVAL = 12;

    /** The {@link #slenderStaticFranticInterval()} a configuration gets when it says nothing. */
    int DEFAULT_SLENDER_STATIC_FRANTIC_INTERVAL = 3;

    /**
     * The longest {@link #slenderStaticQuietInterval()} a configuration may ask for. Past this a
     * round could end before the slender has heard the static twice, which makes it noise rather
     * than a clock.
     */
    int MAX_SLENDER_STATIC_INTERVAL = 120;

    /** The {@link #slenderStaticMinVolume()} a configuration gets when it says nothing. */
    float DEFAULT_SLENDER_STATIC_MIN_VOLUME = 0.15F;

    /** The {@link #slenderStaticMaxVolume()} a configuration gets when it says nothing. */
    float DEFAULT_SLENDER_STATIC_MAX_VOLUME = 0.8F;

    /**
     * The largest {@link #glitchRange()} a configuration may ask for. Beyond this the slender would
     * tear a survivor's view apart from across the map, which is the behaviour this range exists to
     * end.
     */
    int MAX_GLITCH_RANGE = 64;

    /**
     * The widest {@link #glitchViewAngle()} a configuration may ask for. At 90 degrees and beyond
     * the cone stops being a cone: everything not strictly behind the survivor would count as seen,
     * and the effect would no longer be about looking at him.
     */
    int MAX_GLITCH_VIEW_ANGLE = 89;

    /**
     * The {@link #glitchRange()} a configuration gets when it says nothing. Twelve blocks is close
     * enough that the slender is a present threat when the tearing starts - the 32 this used to be
     * kept him inside the range for most of a round, so the effect was near enough permanent and
     * stopped reading as a warning.
     */
    int DEFAULT_GLITCH_RANGE = 12;

    /** The {@link #glitchCloseRange()} a configuration gets when it says nothing. */
    int DEFAULT_GLITCH_CLOSE_RANGE = 4;

    /**
     * The {@link #glitchViewAngle()} a configuration gets when it says nothing. Narrower than the
     * client's field of view on purpose: the cone this replaces spanned roughly 113 degrees and
     * fired while he stood at the very edge of the screen, which is not the same as being looked at.
     */
    int DEFAULT_GLITCH_VIEW_ANGLE = 30;

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
     * Returns whether survivors hear a sound while a page is nearby.
     *
     * @return {@code true} while the proximity hint is on
     * @since 2.12.0
     */
    boolean pageProximityEnabled();

    /**
     * Returns how far away a page may be and still be heard, in blocks.
     * <p>
     * The value doubles as the volume the sound is played at: Minecraft carries a sound
     * {@code 16 * volume} blocks, so a range beyond 16 blocks needs a volume above 1 to reach that
     * far, and the server clips anything past the range itself.
     * </p>
     *
     * @return the range in blocks, at most {@link #MAX_PAGE_PROXIMITY_RANGE}
     * @since 2.12.0
     */
    int pageProximityRange();

    /**
     * Returns the number of ticks between two proximity sounds.
     *
     * @return the interval in ticks, at least 1
     * @since 2.12.0
     */
    int pageProximityInterval();

    /**
     * Returns the sound played while a page is nearby.
     * <p>
     * The key is not resolved against the sound registry here - a key that names no known sound is
     * only noticed when the sound is first played, and the proximity hint falls back to
     * {@link #DEFAULT_PAGE_PROXIMITY_SOUND} then.
     * </p>
     *
     * @return the sound key, never {@code null}
     * @since 2.12.0
     */
    Key pageProximitySound();

    /**
     * Returns how far past {@link #pageProximityRange()} the chime's falloff is stretched.
     * <p>
     * Minecraft carries a sound {@code 16 * volume} blocks and fades it to nothing at that
     * distance, so a volume derived to reach exactly the configured range leaves the chime
     * inaudible at the range's edge. This factor stretches the falloff beyond it; the audible
     * distance is unaffected, because the service drops pages outside the range before playing
     * anything.
     * </p>
     *
     * @return the factor, between 1 and {@link #MAX_PAGE_PROXIMITY_VOLUME_FACTOR}
     * @since 2.12.1
     */
    float pageProximityVolumeFactor();

    /**
     * Returns whether a player hears a sound when they take damage.
     *
     * @return {@code true} while the damage feedback is on
     * @since 2.13.0
     */
    boolean damageSoundEnabled();

    /**
     * Returns how many ticks have to pass before a player hears the damage sound again.
     * <p>
     * The slender damages everyone around him twice a second for as long as he drains, so without
     * a cooldown a survivor standing next to him would hear the sound at that rate.
     * </p>
     *
     * @return the cooldown in ticks, at least 1
     * @since 2.13.0
     */
    int damageSoundCooldown();

    /**
     * Returns the sound played to a player who was just hit.
     * <p>
     * The key is not resolved against the sound registry here - a key that names no known sound is
     * only noticed when the sound is first played, and the feedback falls back to
     * {@link #DEFAULT_DAMAGE_SOUND} then.
     * </p>
     *
     * @return the sound key, never {@code null}
     * @since 2.13.0
     */
    Key damageSound();

    /**
     * Returns how far the lobby's atmosphere is taken from the open end towards the game map's own.
     * <p>
     * {@code 0} leaves the lobby on the vanilla overworld, which is where it was. {@code 1} gives it
     * exactly the map's atmosphere, which makes the start of a round invisible - the point of the
     * setting is the distance between the two, so that walking into the round reads as the world
     * closing in rather than as a cut.
     * </p>
     *
     * @return the share, between 0 and 1
     * @since 2.14.0
     */
    float lobbyAtmosphereShare();

    /**
     * Returns whether the slender hears static as the survivors collect his pages.
     *
     * @return {@code true} while the static is on
     * @since 2.14.0
     */
    boolean slenderStaticEnabled();

    /**
     * Returns the sound the static is built from.
     * <p>
     * The key is not resolved against the sound registry here: a resource pack sound is a perfectly
     * good answer and would not be found in it. It is sent as named.
     * </p>
     *
     * @return the sound key, never {@code null}
     * @since 2.14.0
     */
    Key slenderStaticSound();

    /**
     * Returns how many seconds lie between two bursts while no page has been found.
     *
     * @return the interval in seconds, at most {@link #MAX_SLENDER_STATIC_INTERVAL}
     * @since 2.14.0
     */
    int slenderStaticQuietInterval();

    /**
     * Returns how many seconds lie between two bursts once every page is gone.
     * <p>
     * The gap shrinks from {@link #slenderStaticQuietInterval()} towards this value as the pages
     * disappear, which is what tells the slender how late in the round he is.
     * </p>
     *
     * @return the interval in seconds, below {@link #slenderStaticQuietInterval()}
     * @since 2.14.0
     */
    int slenderStaticFranticInterval();

    /**
     * Returns how loud the static is while no page has been found.
     *
     * @return the volume, between 0 and {@link #slenderStaticMaxVolume()}
     * @since 2.14.0
     */
    float slenderStaticMinVolume();

    /**
     * Returns how loud the static is once every page is gone.
     *
     * @return the volume, at most 1
     * @since 2.14.0
     */
    float slenderStaticMaxVolume();

    /**
     * Returns how close the slender has to be before the sight of him tears a survivor's view.
     * <p>
     * This is the outer edge of the effect, not the point where it is strongest: at exactly this
     * distance a survivor gets the weakest level, and it grows the nearer he comes until
     * {@link #glitchCloseRange()} is reached. Beyond it there is nothing at all - no veil, and no
     * darkening of the world.
     * </p>
     *
     * @return the range in blocks, at most {@link #MAX_GLITCH_RANGE}
     * @since 2.13.0
     */
    int glitchRange();

    /**
     * Returns the distance at which the tearing is at its worst.
     * <p>
     * Always smaller than {@link #glitchRange()} - the two mark the ends of the same slope, and a
     * configuration where they meet or cross is rejected outright.
     * </p>
     *
     * @return the distance in blocks, at least 1 and below {@link #glitchRange()}
     * @since 2.13.0
     */
    int glitchCloseRange();

    /**
     * Returns how far off the centre of their view the slender may stand and still count as seen.
     * <p>
     * Given in degrees around the survivor's line of sight, so a value of 30 means he has to be
     * within 30 degrees of where they are actually looking. This is deliberately narrower than the
     * client's field of view: standing at the very edge of the screen is not the same as being
     * looked at.
     * </p>
     *
     * @return the half-angle in degrees, between 1 and {@link #MAX_GLITCH_VIEW_ANGLE}
     * @since 2.13.0
     */
    int glitchViewAngle();

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
         * Sets whether survivors hear a sound while a page is nearby.
         *
         * @param pageProximityEnabled {@code true} to keep the proximity hint on
         * @return the builder instance
         * @since 2.12.0
         */
        Builder pageProximityEnabled(boolean pageProximityEnabled);

        /**
         * Sets how far away a page may be and still be heard, in blocks.
         *
         * @param pageProximityRange the range in blocks
         * @return the builder instance
         * @throws IllegalArgumentException if the range is below 1 or above
         *                                  {@link GameConfig#MAX_PAGE_PROXIMITY_RANGE}
         * @since 2.12.0
         */
        Builder pageProximityRange(int pageProximityRange);

        /**
         * Sets the number of ticks between two proximity sounds.
         *
         * @param pageProximityInterval the interval in ticks
         * @return the builder instance
         * @throws IllegalArgumentException if the interval is below 1
         * @since 2.12.0
         */
        Builder pageProximityInterval(int pageProximityInterval);

        /**
         * Sets the sound played while a page is nearby.
         *
         * @param pageProximitySound the sound key
         * @return the builder instance
         * @since 2.12.0
         */
        Builder pageProximitySound(Key pageProximitySound);

        /**
         * Sets how far past the range the chime's falloff is stretched.
         *
         * @param pageProximityVolumeFactor the factor
         * @return the builder instance
         * @throws IllegalArgumentException if the factor is below 1 or above
         *                                  {@link GameConfig#MAX_PAGE_PROXIMITY_VOLUME_FACTOR}
         * @since 2.12.1
         */
        Builder pageProximityVolumeFactor(float pageProximityVolumeFactor);

        /**
         * Sets whether a player hears a sound when they take damage.
         *
         * @param damageSoundEnabled {@code true} to keep the damage feedback on
         * @return the builder instance
         * @since 2.13.0
         */
        Builder damageSoundEnabled(boolean damageSoundEnabled);

        /**
         * Sets how many ticks have to pass before a player hears the damage sound again.
         *
         * @param damageSoundCooldown the cooldown in ticks
         * @return the builder instance
         * @throws IllegalArgumentException if the cooldown is below 1
         * @since 2.13.0
         */
        Builder damageSoundCooldown(int damageSoundCooldown);

        /**
         * Sets the sound played to a player who was just hit.
         *
         * @param damageSound the sound key
         * @return the builder instance
         * @since 2.13.0
         */
        Builder damageSound(Key damageSound);

        /**
         * Sets how far the lobby's atmosphere is taken towards the game map's own.
         *
         * @param lobbyAtmosphereShare the share
         * @return the builder instance
         * @throws IllegalArgumentException if the share is below 0 or above 1
         * @since 2.14.0
         */
        Builder lobbyAtmosphereShare(float lobbyAtmosphereShare);

        /**
         * Sets whether the slender hears static as the survivors collect his pages.
         *
         * @param slenderStaticEnabled {@code true} to keep the static on
         * @return the builder instance
         * @since 2.14.0
         */
        Builder slenderStaticEnabled(boolean slenderStaticEnabled);

        /**
         * Sets the sound the static is built from.
         *
         * @param slenderStaticSound the sound key
         * @return the builder instance
         * @since 2.14.0
         */
        Builder slenderStaticSound(Key slenderStaticSound);

        /**
         * Sets how many seconds lie between two bursts while no page has been found.
         *
         * @param slenderStaticQuietInterval the interval in seconds
         * @return the builder instance
         * @throws IllegalArgumentException if the interval is below 1 or above
         *                                  {@link GameConfig#MAX_SLENDER_STATIC_INTERVAL}
         * @since 2.14.0
         */
        Builder slenderStaticQuietInterval(int slenderStaticQuietInterval);

        /**
         * Sets how many seconds lie between two bursts once every page is gone.
         *
         * @param slenderStaticFranticInterval the interval in seconds
         * @return the builder instance
         * @throws IllegalArgumentException if the interval is below 1
         * @since 2.14.0
         */
        Builder slenderStaticFranticInterval(int slenderStaticFranticInterval);

        /**
         * Sets how loud the static is while no page has been found.
         *
         * @param slenderStaticMinVolume the volume
         * @return the builder instance
         * @throws IllegalArgumentException if the volume is below 0 or above 1
         * @since 2.14.0
         */
        Builder slenderStaticMinVolume(float slenderStaticMinVolume);

        /**
         * Sets how loud the static is once every page is gone.
         *
         * @param slenderStaticMaxVolume the volume
         * @return the builder instance
         * @throws IllegalArgumentException if the volume is below 0 or above 1
         * @since 2.14.0
         */
        Builder slenderStaticMaxVolume(float slenderStaticMaxVolume);

        /**
         * Sets how close the slender has to be before the sight of him tears a survivor's view.
         *
         * @param glitchRange the range in blocks
         * @return the builder instance
         * @throws IllegalArgumentException if the range is below 1 or above
         *                                  {@link GameConfig#MAX_GLITCH_RANGE}
         * @since 2.13.0
         */
        Builder glitchRange(int glitchRange);

        /**
         * Sets the distance at which the tearing is at its worst.
         *
         * @param glitchCloseRange the distance in blocks
         * @return the builder instance
         * @throws IllegalArgumentException if the distance is below 1
         * @since 2.13.0
         */
        Builder glitchCloseRange(int glitchCloseRange);

        /**
         * Sets how far off the centre of their view the slender may stand and still count as seen.
         *
         * @param glitchViewAngle the half-angle in degrees
         * @return the builder instance
         * @throws IllegalArgumentException if the angle is below 1 or above
         *                                  {@link GameConfig#MAX_GLITCH_VIEW_ANGLE}
         * @since 2.13.0
         */
        Builder glitchViewAngle(int glitchViewAngle);

        /**
         * Builds the game configuration.
         *
         * @return the created configuration
         */
        GameConfig build();
    }
}
