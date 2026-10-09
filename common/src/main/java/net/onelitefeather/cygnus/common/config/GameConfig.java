package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.regex.Pattern;

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
 * @param pageGlitch           how the slender's own screen tears as pages are found
 * @param creek                the creek, the second figure next to the slender
 * @param sanity               the survivors' fear, which the creek reads
 * @param stamina              the survivors' sprint and the slender's appearing
 * @param adrenaline           the rush a survivor gets when the visible slender comes close
 * @param footprint            the prints the hidden slender leaves and the survivor tracks the slender can reveal
 * @param possession           the slender's look through the eyes of the creek
 * @param telemetry            the OpenTelemetry traces, which only do anything with the javaagent attached
 * @param minimap              the request to client minimap mods to switch themselves off
 * @param lobbyAtmosphereShare how far the lobby's atmosphere is taken towards the map's own: {@code 0}
 *                             leaves the vanilla overworld, {@code 1} is exactly the map's atmosphere
 * @author theEvilReaper
 * @version 2.5.0
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
        PageGlitch pageGlitch,
        CreekConfig creek,
        SanityConfig sanity,
        StaminaConfig stamina,
        AdrenalineConfig adrenaline,
        FootprintConfig footprint,
        PossessionConfig possession,
        TelemetryConfig telemetry,
        MinimapConfig minimap,
        float lobbyAtmosphereShare
) {

    public static final String SLENDER_TEAM_NAME = "Slender";
    public static final Key SLENDER_KEY = Key.key("cygnus", "slender");
    public static final String SURVIVOR_TEAM_NAME = "Survivor";
    public static final Key SURVIVOR_KEY = Key.key("cygnus", "survivor");
    public static final Key SPECTATOR_KEY = Key.key("cygnus", "spectator");

    /**
     * How many pages are in the world at once, at least. Playtest tuning: at most four pages are out at the
     * same time; the total number to find was doubled separately.
     */
    public static final int MIN_ACTIVE_PAGE_COUNT = 4;

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

    /**
     * The fewest pages a round needs to find. Doubled from 8 to 16: playtesters wanted twice as many pages.
     */
    public static final int MIN_PAGE_COUNT = 16;

    /**
     * The {@link #lobbyAtmosphereShare()} a configuration gets when it says nothing: enough of the
     * map's haze to be recognised in the distance, while the lobby still reads as the lit room
     * players wait in.
     */
    public static final float DEFAULT_LOBBY_ATMOSPHERE_SHARE = 0.3F;

    private static final Logger LOGGER = LoggerFactory.getLogger(GameConfig.class);

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
            PageGlitch.DEFAULT,
            CreekConfig.DEFAULT,
            SanityConfig.DEFAULT,
            StaminaConfig.DEFAULT,
            AdrenalineConfig.DEFAULT,
            FootprintConfig.DEFAULT,
            PossessionConfig.DEFAULT,
            TelemetryConfig.DEFAULT,
            MinimapConfig.DEFAULT,
            DEFAULT_LOBBY_ATMOSPHERE_SHARE
    );

    public GameConfig {
        if (lobbyAtmosphereShare < 0.0F || lobbyAtmosphereShare > 1.0F) {
            throw new IllegalArgumentException("Lobby atmosphere share must be between 0 and 1");
        }
    }

    /**
     * Reads the whole config. Every group reads its own keys, see the {@code read} method of each. A group with invalid
     * values falls back to its defaults with a warning.
     *
     * @param root the whole config
     * @return the game configuration
     */
    public static GameConfig read(ConfigSection root) {
        return new GameConfig(
                Round.read(root),
                Teams.read(root),
                root.getString("sentryDsn"),
                ResourcePack.read(root),
                PageProximity.read(root),
                DamageSound.read(root),
                Glitch.read(root),
                PageGlitch.read(root),
                CreekConfig.read(root.section("creek.")),
                SanityConfig.read(root.section("sanity.")),
                StaminaConfig.read(root.section("stamina.")),
                AdrenalineConfig.read(root.section("adrenaline.")),
                FootprintConfig.read(root.section("footprint.")),
                PossessionConfig.read(root.section("possession.")),
                TelemetryConfig.read(root.section("telemetry.")),
                MinimapConfig.read(root.section("minimap.")),
                readLobbyAtmosphereShare(root)
        );
    }

    /**
     * Reads how far the lobby takes on the map's atmosphere. A share outside 0 to 1 falls back to
     * the default with a warning.
     *
     * @param root the whole config
     * @return the share
     */
    private static float readLobbyAtmosphereShare(ConfigSection root) {
        float share = root.getFloat("lobbyAtmosphereShare", DEFAULT_LOBBY_ATMOSPHERE_SHARE);
        if (share < 0.0F || share > 1.0F) {
            LOGGER.warn("Invalid values for lobby atmosphere share: {} is not between 0 and 1. Falling back to its defaults", share);
            return DEFAULT_LOBBY_ATMOSPHERE_SHARE;
        }
        return share;
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

        /**
         * Reads the player limits and timings, keys without a prefix.
         *
         * @param root the whole config
         * @return the round settings
         */
        public static Round read(ConfigSection root) {
            return root.orDefault("round", () -> new Round(
                    root.getInt("minPlayers", DEFAULT.minPlayers()),
                    root.getInt("maxPlayers", DEFAULT.maxPlayers()),
                    root.getInt("lobbyTime", DEFAULT.lobbyTime()),
                    root.getInt("gameTime", DEFAULT.gameTime())), DEFAULT);
        }
    }

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

        /**
         * Reads the team sizes, keys without a prefix.
         *
         * @param root the whole config
         * @return the team sizes
         */
        public static Teams read(ConfigSection root) {
            return root.orDefault("teams", () -> new Teams(
                    root.getInt("slenderTeamSize", DEFAULT.slenderSize()),
                    root.getInt("survivorTeamSize", DEFAULT.survivorSize())), DEFAULT);
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

        private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePack.class);
        private static final Pattern SHA1_PATTERN = Pattern.compile("[0-9a-fA-F]{40}");

        /**
         * Reads where the pack comes from, keys without a prefix. A broken URL turns the feature
         * off and a malformed checksum is dropped, instead of failing the start.
         *
         * @param root the whole config
         * @return the pack location
         */
        public static ResourcePack read(ConfigSection root) {
            return new ResourcePack(readUrl(root), readSha1(root));
        }

        /**
         * Reads the pack location. A value that is not a valid URI turns the feature off.
         *
         * @param root the whole config
         * @return the parsed URL, or {@code null} if it is absent or unusable
         */
        private static @Nullable URI readUrl(ConfigSection root) {
            String value = root.getString("resourcePackUrl");
            if (value == null) {
                return null;
            }
            try {
                return URI.create(value);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("'{}' is not a valid URI: '{}'. Disabling the ResourcePack feature", "resourcePackUrl", value, exception);
                return null;
            }
        }

        /**
         * Reads the pack checksum. Anything that is not 40 hexadecimal characters is not a SHA-1 and
         * is dropped, which leaves the checksum to be computed from the pack at runtime.
         *
         * @param root the whole config
         * @return the checksum, or {@code null} if it is absent or malformed
         */
        private static @Nullable String readSha1(ConfigSection root) {
            String value = root.getString("resourcePackSha1");
            if (value == null) {
                return null;
            }
            if (!SHA1_PATTERN.matcher(value).matches()) {
                LOGGER.warn("'{}' is not a SHA-1 checksum: '{}'. It will be computed from the pack instead", "resourcePackSha1", value);
                return null;
            }
            return value;
        }
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

        /**
         * Reads the page hint sound, keys without a prefix.
         *
         * @param root the whole config
         * @return the page hint settings
         */
        public static PageProximity read(ConfigSection root) {
            return root.orDefault("page proximity", () -> new PageProximity(
                    root.getBoolean("pageProximityEnabled", DEFAULT.enabled()),
                    root.getInt("pageProximityRange", DEFAULT.range()),
                    root.getSound("pageProximitySound", DEFAULT.sound()),
                    root.getFloat("pageProximityVolumeFactor", DEFAULT.volumeFactor())), DEFAULT);
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

        /**
         * Reads the hit sound, keys without a prefix.
         *
         * @param root the whole config
         * @return the hit sound settings
         */
        public static DamageSound read(ConfigSection root) {
            return root.orDefault("damage sound", () -> new DamageSound(
                    root.getBoolean("damageSoundEnabled", DEFAULT.enabled()),
                    root.getInt("damageSoundCooldown", DEFAULT.cooldown()),
                    root.getSound("damageSound", DEFAULT.sound())), DEFAULT);
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

        /**
         * Reads how the slender's sight tears a survivor's view, keys without a prefix.
         *
         * @param root the whole config
         * @return the glitch settings
         */
        public static Glitch read(ConfigSection root) {
            return root.orDefault("glitch", () -> new Glitch(
                    root.getInt("glitchRange", DEFAULT.range()),
                    root.getInt("glitchCloseRange", DEFAULT.closeRange()),
                    root.getInt("glitchViewAngle", DEFAULT.viewAngle())), DEFAULT);
        }
    }

    /**
     * The glitch on the slender's own screen while the survivors take his pages away. It is the
     * only thing that tells him how far they have got without putting the page counter in front of
     * him. A baseline eases in as the pages disappear, staying low for the first half of
     * them and climbing late so the worst level is kept for the end, and every find pulses one
     * level above it. Both are capped at {@code maxLevel}: playtests showed that the higher levels
     * left the slender blind to the very players he hunts, so by default he gets the weakest level
     * only and the pulse has no visible step above it.
     *
     * @param enabled      whether the slender's screen tears at all
     * @param pulseSeconds how long a single find holds the glitch one level above the round's
     *                     level, in seconds
     * @param maxLevel     the strongest level his screen may reach, baseline and pulse alike,
     *                     between {@code 0} (the weakest) and {@link #MAX_LEVEL}
     */
    public record PageGlitch(boolean enabled, int pulseSeconds, int maxLevel) {

        /**
         * The strongest level a cap may allow. Mirrors the gaze's level count minus one; the common
         * module cannot see the game's {@code SlenderGaze}, so the two are kept in step by hand.
         */
        public static final int MAX_LEVEL = 3;

        /** The cap a configuration gets when it says nothing: the weakest level only. */
        public static final int DEFAULT_MAX_LEVEL = 0;

        /** The pulse a configuration gets when it says nothing. */
        public static final int DEFAULT_PULSE_SECONDS = 3;

        /**
         * Past this a find late in the round would still be on screen when the next one lands, and
         * the pulse stops reading as an event.
         */
        public static final int MAX_PULSE_SECONDS = 30;

        public static final PageGlitch DEFAULT = new PageGlitch(true, DEFAULT_PULSE_SECONDS, DEFAULT_MAX_LEVEL);

        /**
         * Creates a page glitch with the default cap.
         *
         * @param enabled      whether the slender's screen tears at all
         * @param pulseSeconds how long a single find holds the pulse, in seconds
         */
        public PageGlitch(boolean enabled, int pulseSeconds) {
            this(enabled, pulseSeconds, DEFAULT_MAX_LEVEL);
        }

        public PageGlitch {
            if (maxLevel < 0 || maxLevel > MAX_LEVEL) {
                throw new IllegalArgumentException("Page glitch max level must be between 0 and " + MAX_LEVEL);
            }
            if (pulseSeconds < 1 || pulseSeconds > MAX_PULSE_SECONDS) {
                throw new IllegalArgumentException(
                        "Page glitch pulse must be between 1 and " + MAX_PULSE_SECONDS + " seconds");
            }
        }

        /**
         * Reads how the slender's screen tears as pages are found, keys without a prefix.
         *
         * @param root the whole config
         * @return the page glitch settings
         */
        public static PageGlitch read(ConfigSection root) {
            return root.orDefault("page glitch", () -> new PageGlitch(
                    root.getBoolean("pageGlitchEnabled", DEFAULT.enabled()),
                    root.getInt("pageGlitchPulseSeconds", DEFAULT.pulseSeconds()),
                    root.getInt("pageGlitchMaxLevel", DEFAULT.maxLevel())), DEFAULT);
        }
    }
}
