package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The configuration of a game, grouped by the feature each value belongs to.
 * <p>
 * Every group checks its own values when it is created, so a configuration read from the file, the
 * defaults and one built in a test go through the same rules. The static values are constants of
 * the game itself and cannot be configured.
 * </p>
 *
 * @param round                the player limits and timings of a round
 * @param teams                the team sizes
 * @param sentryDsn            the DSN Sentry reports to, or {@code null} to keep the integration off
 * @param resourcePack         where the client gets the ResourcePack from
 * @param pageProximity        the sound that hints at a nearby page
 * @param damageSound          the sound a player hears when hit
 * @param glitch               how the sight of the slender tears a survivor's view
 * @param slenderStatic        the static the slender hears while pages are found
 * @param lobbyAtmosphereShare how far the lobby's atmosphere is taken towards the map's own: {@code 0}
 *                             leaves the vanilla overworld, {@code 1} is exactly the map's atmosphere
 * @author theEvilReaper
 * @version 2.0.0
 * @since 1.0.0
 */
public record GameConfig(
        Round round,
        Teams teams,
        @Nullable String sentryDsn,
        ResourcePack resourcePack,
        PageProximity pageProximity,
        DamageSound damageSound,
        Glitch glitch,
        SlenderStatic slenderStatic,
        float lobbyAtmosphereShare
) {

    public static final String SLENDER_TEAM_NAME = "Slender";
    public static final Key SLENDER_KEY = Key.key("cygnus", "slender");
    public static final String SURVIVOR_TEAM_NAME = "Survivor";
    public static final Key SURVIVOR_KEY = Key.key("cygnus", "survivor");
    public static final Key SPECTATOR_KEY = Key.key("cygnus", "spectator");

    public static final int MIN_ACTIVE_PAGE_COUNT = 4 * 2;

    /**
     * How many seconds after a round starts before the first pages spawn.
     * <p>
     * Spawning immediately at {@code GameStartEvent} let survivors grab a page before they had even
     * moved from the spawn point. The delay gives them time to spread across the map first.
     * </p>
     */
    public static final int PAGE_SPAWN_DELAY = 10;

    /**
     * How many seconds {@link #PAGE_SPAWN_DELAY} may randomly shift up or down, re-rolled every
     * round, so the moment the first pages appear stays unpredictable.
     */
    public static final int PAGE_SPAWN_DELAY_JITTER = 2;

    /**
     * How many seconds a found page stays hidden when there is no free spot left to move it to.
     * <p>
     * The page then has to come back on the spot it was found on. Without the delay it would be
     * collectible again right away, letting a survivor pick up page after page on the same spot.
     * </p>
     */
    public static final int PAGE_RESPAWN_DELAY = 15;

    /**
     * How many seconds {@link #PAGE_RESPAWN_DELAY} may randomly shift up or down, re-rolled for every
     * hidden page, so waiting next to the spot does not pay off.
     */
    public static final int PAGE_RESPAWN_DELAY_JITTER = 5;

    public static final int PAGE_TTL_TIME = 60;
    public static final int FORCE_START_TIME = 11;
    public static final int MIN_PAGE_COUNT = 8;

    /**
     * The {@link #lobbyAtmosphereShare()} a configuration gets when it says nothing: enough of the
     * map's haze to be recognised in the distance, while the lobby still reads as the lit room
     * players wait in.
     */
    public static final float DEFAULT_LOBBY_ATMOSPHERE_SHARE = 0.3F;

    /**
     * The configuration used when there is no config file, or nothing in it can be read.
     * <p>
     * Sentry and the ResourcePack are opt-in, so a local run reports to nothing and pushes nothing.
     * The hints and effects are on, since a round without them plays worse, not differently.
     * </p>
     */
    public static final GameConfig DEFAULT = new GameConfig(
            Round.DEFAULT,
            Teams.DEFAULT,
            null,
            ResourcePack.NONE,
            PageProximity.DEFAULT,
            DamageSound.DEFAULT,
            Glitch.DEFAULT,
            SlenderStatic.DEFAULT,
            DEFAULT_LOBBY_ATMOSPHERE_SHARE
    );

    public GameConfig {
        if (lobbyAtmosphereShare < 0.0F || lobbyAtmosphereShare > 1.0F) {
            throw new IllegalArgumentException("Lobby atmosphere share must be between 0 and 1");
        }
    }

    /**
     * The player limits and timings of a round.
     *
     * @param minPlayers the number of players needed to start the countdown
     * @param maxPlayers the number of players allowed in a round
     * @param lobbyTime  the countdown in seconds, longer than {@link #FORCE_START_TIME}
     * @param gameTime   the length of a round in seconds
     */
    public record Round(int minPlayers, int maxPlayers, int lobbyTime, int gameTime) {

        public static final Round DEFAULT = new Round(2, 13, 30, 900);

        public Round {
            if (lobbyTime <= FORCE_START_TIME) {
                throw new IllegalArgumentException("Lobby time must be greater than " + FORCE_START_TIME);
            }
        }
    }

    /**
     * Returns the settings of the creek, the figure that walks the map next to the slender.
     *
     * @return the creek settings, never {@code null}
     */
    CreekConfig creek();

    /**
     * The sizes of the teams.
     *
     * @param slenderSize  the size of the slender team, at least 1
     * @param survivorSize the size of the survivor team, larger than the slender team's minimum
     */
    public record Teams(int slenderSize, int survivorSize) {

        private static final int MIN_SLENDER_SIZE = 1;

        public static final Teams DEFAULT = new Teams(1, 12);

        public Teams {
            if (slenderSize < MIN_SLENDER_SIZE) {
                throw new IllegalArgumentException("Slender team size must be at least " + MIN_SLENDER_SIZE);
            }
            if (survivorSize < MIN_SLENDER_SIZE + 1) {
                throw new IllegalArgumentException("Survivor team size must be at least " + (MIN_SLENDER_SIZE + 1));
            }
        }
    }

    /**
     * Where the client gets the ResourcePack from.
     *
     * @param url  the location of the pack, or {@code null} to keep the feature off
     * @param sha1 the checksum the client verifies the pack against, or {@code null} to compute it
     *             from the pack at runtime. A production setup always states it: it lets a client
     *             reuse the pack it already has instead of downloading it on every join.
     */
    public record ResourcePack(@Nullable URI url, @Nullable String sha1) {

        public static final ResourcePack NONE = new ResourcePack(null, null);
    }

    /**
     * The sound survivors hear while a page is nearby.
     *
     * @param enabled      whether the hint is played at all
     * @param range        how far away a page may be and still be heard, in blocks
     * @param sound        the sound; a key naming no known sound falls back to {@link #DEFAULT_SOUND}
     *                     when it is first played
     * @param volumeFactor how far past the range the falloff is stretched. Minecraft fades a sound to
     *                     nothing at {@code 16 * volume} blocks, so a volume that reaches exactly the
     *                     range would be silent at its edge.
     */
    public record PageProximity(boolean enabled, int range, Key sound, float volumeFactor) {

        /** A soft, bell-less shimmer that reads as "something is here" without sounding like an alarm. */
        public static final Key DEFAULT_SOUND = Key.key("block.amethyst_block.chime");

        /** Beyond this a single page would be audible across a good part of the map. */
        public static final int MAX_RANGE = 64;

        /** Leaves roughly half the volume at the edge of the range. */
        public static final float DEFAULT_VOLUME_FACTOR = 2.0F;

        /**
         * Past this the chime is evenly loud across the whole range, so a player can no longer tell a
         * page two blocks away from one at the edge.
         */
        public static final float MAX_VOLUME_FACTOR = 8.0F;

        public static final PageProximity DEFAULT = new PageProximity(true, 20, DEFAULT_SOUND, DEFAULT_VOLUME_FACTOR);

        public PageProximity {
            if (range < 1 || range > MAX_RANGE) {
                throw new IllegalArgumentException("Page proximity range must be between 1 and " + MAX_RANGE);
            }
            if (volumeFactor < 1.0F || volumeFactor > MAX_VOLUME_FACTOR) {
                throw new IllegalArgumentException("Page proximity volume factor must be between 1 and " + MAX_VOLUME_FACTOR);
            }
        }
    }

    /**
     * The sound a player hears when hit.
     *
     * @param enabled  whether the sound is played at all
     * @param cooldown the ticks before a player hears it again, at least 1. The slender damages
     *                 everyone around him twice a second while he drains.
     * @param sound    the sound; a key naming no known sound falls back to {@link #DEFAULT_SOUND}
     *                 when it is first played
     */
    public record DamageSound(boolean enabled, int cooldown, Key sound) {

        /**
         * The vanilla hurt sound: Cygnus sets health directly, which never runs Minestom's damage
         * pipeline and so never plays the sound a client would otherwise hear.
         */
        public static final Key DEFAULT_SOUND = Key.key("entity.player.hurt");

        /** Lets through every second damage tick of a draining slender: enough to notice, not enough to grate. */
        public static final DamageSound DEFAULT = new DamageSound(true, 20, DEFAULT_SOUND);

        public DamageSound {
            if (cooldown < 1) {
                throw new IllegalArgumentException("Damage sound cooldown must be at least 1 tick");
            }
        }
    }

    /**
     * How the sight of the slender tears a survivor's view.
     *
     * @param range      the outer edge of the effect in blocks; the level grows from here towards the
     *                   close range, and beyond it there is nothing at all
     * @param closeRange the distance at which the tearing is at its worst, below the range
     * @param viewAngle  how far off the centre of the view the slender may stand and still count as
     *                   seen, in degrees. Narrower than the client's field of view on purpose: the edge
     *                   of the screen is not the same as being looked at.
     */
    public record Glitch(int range, int closeRange, int viewAngle) {

        /** Beyond this the slender would tear a survivor's view apart from across the map. */
        public static final int MAX_RANGE = 64;

        /** At 90 degrees and beyond everything not strictly behind the survivor would count as seen. */
        public static final int MAX_VIEW_ANGLE = 89;

        /** Twelve blocks keeps the effect a warning; the 32 this used to be made it near enough permanent. */
        public static final Glitch DEFAULT = new Glitch(12, 4, 30);

        public Glitch {
            if (range < 1 || range > MAX_RANGE) {
                throw new IllegalArgumentException("Glitch range must be between 1 and " + MAX_RANGE);
            }
            if (closeRange < 1) {
                throw new IllegalArgumentException("Glitch close range must be at least 1 block");
            }
            if (viewAngle < 1 || viewAngle > MAX_VIEW_ANGLE) {
                throw new IllegalArgumentException("Glitch view angle must be between 1 and " + MAX_VIEW_ANGLE + " degrees");
            }
            if (closeRange >= range) {
                throw new IllegalArgumentException(
                        "Glitch close range (" + closeRange + ") must be below the glitch range (" + range + ")");
            }
        }
    }

    /**
     * The static the slender hears while the survivors take his pages away. It is the only thing
     * that tells him how far they have got without putting the page counter in front of him.
     *
     * @param enabled         whether the static is played at all
     * @param sound           the sound, sent as named: a resource pack sound would not be found in the
     *                        registry
     * @param quietInterval   the seconds between two bursts while no page has been found
     * @param franticInterval the seconds between two bursts once every page is gone, below the quiet
     *                        interval
     * @param minVolume       the volume while no page has been found
     * @param maxVolume       the volume once every page is gone
     */
    public record SlenderStatic(
            boolean enabled,
            Key sound,
            int quietInterval,
            int franticInterval,
            float minVolume,
            float maxVolume
    ) {

        /**
         * Three takes of tape hiss from the Cygnus resource pack. A server without the pack hears
         * nothing here, which beats half a horror cue played through the wrong sample.
         */
        public static final Key DEFAULT_SOUND = Key.key("cygnus", "vhs_static");

        /** Past this a round could end before the slender has heard the static twice. */
        public static final int MAX_INTERVAL = 120;

        public static final SlenderStatic DEFAULT = new SlenderStatic(true, DEFAULT_SOUND, 12, 3, 0.15F, 0.8F);

        public SlenderStatic {
            if (quietInterval < 1 || quietInterval > MAX_INTERVAL) {
                throw new IllegalArgumentException(
                        "Slender static quiet interval must be between 1 and " + MAX_INTERVAL + " seconds");
            }
            if (franticInterval < 1) {
                throw new IllegalArgumentException("Slender static frantic interval must be at least 1 second");
            }
            checkVolume(minVolume, "minimum");
            checkVolume(maxVolume, "maximum");
            if (franticInterval >= quietInterval) {
                throw new IllegalArgumentException(
                        "Slender static frantic interval (" + franticInterval
                                + ") must be below the quiet interval (" + quietInterval + ")");
            }
            if (minVolume > maxVolume) {
                throw new IllegalArgumentException(
                        "Slender static minimum volume (" + minVolume
                                + ") must not be above the maximum volume (" + maxVolume + ")");
            }
        }

        private static void checkVolume(float volume, String name) {
            if (volume < 0.0F || volume > 1.0F) {
                throw new IllegalArgumentException("Slender static " + name + " volume must be between 0 and 1");
            }
        }
    }
}
