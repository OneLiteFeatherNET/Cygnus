package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.below;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notAbove;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.positive;

/**
 * Settings for the creek, the second figure next to the slender.
 * <p>
 * All numbers that control the creek's behavior live here, so they can be tuned in a playtest
 * without changing code. They are sorted into groups, but the keys in {@code config.properties}
 * stay flat, for example {@code creek.stalkMinDistance}. Each group rejects values that break it,
 * and this record rejects the combinations that span groups, for example a hiding spot inside the
 * view cone.
 * </p>
 * <p>
 * The distances that depend on the map — sight range, stalk band, respawn distance, route link
 * distance, personal space and launch height — live in {@link CreekSettings}.
 * </p>
 *
 * @param enabled                whether the creek takes part in a round
 * @param activeWithLastSurvivor whether the creek keeps going when only one survivor is left
 * @param stuckMillis            how long the creek may make no progress before taking a shortcut
 * @param sight                  when a survivor notices the creek
 * @param wander                 how the creek moves while patrolling
 * @param stalk                  how the creek follows a scared survivor
 * @param hunt                   how the creek chases a survivor
 * @param vanish                 how long the creek stays away and where it comes back
 * @param catching               what a catch does
 * @author theEvilReaper
 * @version 2.1.0
 * @since 2.15.0
 */
public record CreekConfig(
        boolean enabled,
        boolean activeWithLastSurvivor,
        int stuckMillis,
        Sight sight,
        Wander wander,
        Stalk stalk,
        Hunt hunt,
        Vanish vanish,
        Catch catching
) {

    /**
     * How far off the view centre a teleport target has to be, in degrees, so the jump does not
     * happen on the survivor's screen. The client shows about 51 degrees to each side with the
     * default field of view and up to about 68 with the widest one, so the creek must not appear
     * inside that. Not a setting: it follows from the client, not from how the creek should play.
     */
    public static final int HIDE_ANGLE = 75;

    /**
     * The default settings: enabled, and tuned for a round of about fifteen minutes.
     */
    public static final CreekConfig DEFAULT = new CreekConfig(
            true, true, 3000,
            Sight.DEFAULT, Wander.DEFAULT, Stalk.DEFAULT, Hunt.DEFAULT, Vanish.DEFAULT, Catch.DEFAULT
    );

    /**
     * Checks the top-level values and the combinations that span groups.
     *
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public CreekConfig {
        atLeast("stuckMillis", stuckMillis, 1);
        below("stalkThreshold", stalk.threshold(), "huntThreshold", hunt.threshold());
        // A hiding spot inside the view cone would count as seen right away.
        below("sightViewAngle", sight.viewAngle(), "stalkMinAngle", stalk.minAngle());
    }

    /**
     * Reads the creek settings. The section carries the {@code creek.} prefix. An unreadable value
     * falls back to its default. A group with invalid values falls back to its defaults. Values that
     * contradict each other across groups reset everything but {@code enabled} and
     * {@code activeWithLastSurvivor}. That includes a group whose fallback contradicts the values
     * of another group.
     *
     * @param section the {@code creek.} part of the config
     * @return the creek settings
     */
    public static CreekConfig read(ConfigSection section) {
        boolean enabled = section.getBoolean("enabled", DEFAULT.enabled());
        boolean activeWithLastSurvivor = section.getBoolean("activeWithLastSurvivor", DEFAULT.activeWithLastSurvivor());
        Sight sight = Sight.read(section);
        Wander wander = Wander.read(section);
        Stalk stalk = Stalk.read(section);
        Hunt hunt = Hunt.read(section);
        Vanish vanish = Vanish.read(section);
        Catch catching = Catch.read(section);
        // A contradiction between groups does not tell which side is wrong, so everything but the
        // two switches falls back. A creek the operator switched off has to stay off.
        CreekConfig fallback = new CreekConfig(enabled, activeWithLastSurvivor, DEFAULT.stuckMillis(),
                Sight.DEFAULT, Wander.DEFAULT, Stalk.DEFAULT, Hunt.DEFAULT, Vanish.DEFAULT, Catch.DEFAULT);
        return section.orDefault("creek", () -> new CreekConfig(
                enabled,
                activeWithLastSurvivor,
                section.getInt("stuckMillis", DEFAULT.stuckMillis()),
                sight, wander, stalk, hunt, vanish, catching), fallback);
    }

    /**
     * When a survivor notices the creek.
     *
     * @param viewAngle the maximum angle from a survivor's view center at which the creek counts as
     *                  seen, in degrees (key {@code sightViewAngle})
     */
    public record Sight(int viewAngle) {

        /** The default sight. */
        public static final Sight DEFAULT = new Sight(35);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range
         */
        public Sight {
            between("sightViewAngle", viewAngle, 1, GameConfig.Glitch.MAX_VIEW_ANGLE);
        }

        /**
         * Reads the sight.
         *
         * @param section the {@code creek.} part of the config
         * @return the sight
         */
        public static Sight read(ConfigSection section) {
            return section.orDefault("creek sight", () -> new Sight(
                    section.getInt("sightViewAngle", DEFAULT.viewAngle())), DEFAULT);
        }
    }

    /**
     * How the creek moves while patrolling.
     *
     * @param pauseMillis   how long the creek stops when spotted while wandering (key {@code wanderPauseMillis})
     * @param speed         movement speed while wandering, in blocks per tick (key {@code wanderSpeed})
     * @param stopChance    chance that the creek stops for a moment after reaching a waypoint (key {@code randomStopChance})
     * @param stopMinMillis shortest random stop, in milliseconds (key {@code randomStopMinMillis})
     * @param stopMaxMillis longest random stop, in milliseconds (key {@code randomStopMaxMillis})
     */
    public record Wander(int pauseMillis, double speed, double stopChance, int stopMinMillis, int stopMaxMillis) {

        /** The default wandering. */
        public static final Wander DEFAULT = new Wander(1500, 0.07D, 0.15D, 1500, 4000);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range
         */
        public Wander {
            atLeast("wanderPauseMillis", pauseMillis, 0);
            positive("wanderSpeed", speed);
            between("randomStopChance", stopChance, 0.0D, 1.0D);
            atLeast("randomStopMinMillis", stopMinMillis, 0);
            notAbove("randomStopMinMillis", stopMinMillis, "randomStopMaxMillis", stopMaxMillis);
        }

        /**
         * Reads the wandering.
         *
         * @param section the {@code creek.} part of the config
         * @return the wandering
         */
        public static Wander read(ConfigSection section) {
            return section.orDefault("creek wander", () -> new Wander(
                    section.getInt("wanderPauseMillis", DEFAULT.pauseMillis()),
                    section.getDouble("wanderSpeed", DEFAULT.speed()),
                    section.getDouble("randomStopChance", DEFAULT.stopChance()),
                    section.getInt("randomStopMinMillis", DEFAULT.stopMinMillis()),
                    section.getInt("randomStopMaxMillis", DEFAULT.stopMaxMillis())), DEFAULT);
        }
    }

    /**
     * How the creek follows a scared survivor.
     *
     * @param threshold    the dread at which the creek starts stalking a survivor (key {@code stalkThreshold})
     * @param minAngle     minimum angle from the stalked survivor's view direction, in degrees (key {@code stalkMinAngle})
     * @param maxAngle     maximum angle from the stalked survivor's view direction, in degrees (key {@code stalkMaxAngle})
     * @param revealMillis how long the creek stays visible before it teleports away (key {@code stalkRevealMillis})
     * @param minSeconds   minimum length of a stalk (key {@code stalkMinSeconds})
     * @param maxSeconds   maximum length of a stalk (key {@code stalkMaxSeconds})
     */
    public record Stalk(double threshold, int minAngle, int maxAngle, int revealMillis, int minSeconds,
                        int maxSeconds) {

        /** The default stalk. It appears beside and behind the survivor's shoulder, off their screen. */
        public static final Stalk DEFAULT = new Stalk(0.25D, 80, 130, 700, 45, 90);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range or two values contradict each other
         */
        public Stalk {
            positive("stalkThreshold", threshold);
            below("stalkMinAngle", minAngle, "stalkMaxAngle", maxAngle);
            between("stalkMaxAngle", maxAngle, 1, 180);
            // Spots inside the hide angle are never used, so the band has to reach past it.
            below("hideAngle", HIDE_ANGLE, "stalkMaxAngle", maxAngle);
            atLeast("stalkRevealMillis", revealMillis, 0);
            atLeast("stalkMinSeconds", minSeconds, 1);
            notAbove("stalkMinSeconds", minSeconds, "stalkMaxSeconds", maxSeconds);
        }

        /**
         * Reads the stalk.
         *
         * @param section the {@code creek.} part of the config
         * @return the stalk
         */
        public static Stalk read(ConfigSection section) {
            return section.orDefault("creek stalk", () -> new Stalk(
                    section.getDouble("stalkThreshold", DEFAULT.threshold()),
                    section.getInt("stalkMinAngle", DEFAULT.minAngle()),
                    section.getInt("stalkMaxAngle", DEFAULT.maxAngle()),
                    section.getInt("stalkRevealMillis", DEFAULT.revealMillis()),
                    section.getInt("stalkMinSeconds", DEFAULT.minSeconds()),
                    section.getInt("stalkMaxSeconds", DEFAULT.maxSeconds())), DEFAULT);
        }
    }

    /**
     * How the creek chases a survivor.
     *
     * @param speed           movement speed while hunting, in blocks per tick. Must be above a walking
     *                        survivor's 0.216 (key {@code huntSpeed})
     * @param threshold       the dread at which a stalk turns into a hunt (key {@code huntThreshold})
     * @param maxSeconds      maximum length of a hunt (key {@code huntMaxSeconds})
     * @param minStalkSeconds how long a stalk has to run before it may turn into a hunt (key {@code huntMinStalkSeconds})
     * @param cooldownSeconds how long a survivor is safe from the next hunt after one ends (key {@code huntCooldownSeconds})
     * @param catchDistance   distance at which the creek catches a survivor, in blocks (key {@code catchDistance})
     */
    public record Hunt(double speed, double threshold, int maxSeconds, int minStalkSeconds, int cooldownSeconds,
                       double catchDistance) {

        /** The default hunt. */
        public static final Hunt DEFAULT = new Hunt(0.25D, 0.6D, 30, 10, 45, 1.5D);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range
         */
        public Hunt {
            positive("huntSpeed", speed);
            between("huntThreshold", threshold, 0.0D, 1.0D);
            atLeast("huntMaxSeconds", maxSeconds, 1);
            atLeast("huntMinStalkSeconds", minStalkSeconds, 0);
            atLeast("huntCooldownSeconds", cooldownSeconds, 0);
            positive("catchDistance", catchDistance);
        }

        /**
         * Reads the hunt.
         *
         * @param section the {@code creek.} part of the config
         * @return the hunt
         */
        public static Hunt read(ConfigSection section) {
            return section.orDefault("creek hunt", () -> new Hunt(
                    section.getDouble("huntSpeed", DEFAULT.speed()),
                    section.getDouble("huntThreshold", DEFAULT.threshold()),
                    section.getInt("huntMaxSeconds", DEFAULT.maxSeconds()),
                    section.getInt("huntMinStalkSeconds", DEFAULT.minStalkSeconds()),
                    section.getInt("huntCooldownSeconds", DEFAULT.cooldownSeconds()),
                    section.getDouble("catchDistance", DEFAULT.catchDistance())), DEFAULT);
        }
    }

    /**
     * How long the creek stays away and where it comes back.
     *
     * @param minSeconds         shortest vanish time, used at full dread (key {@code vanishMinSeconds})
     * @param maxSeconds         longest vanish time, used at no dread (key {@code vanishMaxSeconds})
     */
    public record Vanish(int minSeconds, int maxSeconds) {

        /** The default vanish. */
        public static final Vanish DEFAULT = new Vanish(20, 40);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range or two values contradict each other
         */
        public Vanish {
            atLeast("vanishMinSeconds", minSeconds, 0);
            notAbove("vanishMinSeconds", minSeconds, "vanishMaxSeconds", maxSeconds);
        }

        /**
         * Reads the vanish.
         *
         * @param section the {@code creek.} part of the config
         * @return the vanish
         */
        public static Vanish read(ConfigSection section) {
            return section.orDefault("creek vanish", () -> new Vanish(
                    section.getInt("vanishMinSeconds", DEFAULT.minSeconds()),
                    section.getInt("vanishMaxSeconds", DEFAULT.maxSeconds())), DEFAULT);
        }
    }

    /**
     * What a catch does.
     *
     * @param betrayalCatchCount  from this catch on, a survivor is always revealed to the slender
     * @param betrayalChance      chance of a reveal on earlier catches
     * @param betrayalGlowSeconds how long a revealed survivor shows on the slender's locator bar
     * @param slownessSeconds     how long a caught survivor is slowed
     * @param swapChance          chance that a catch swaps two survivors instead of throwing the caught one
     *                            {@link CreekSettings#launchHeight()} blocks into the air.
     *                            0 always throws, 1 always swaps
     * @param launchDamage        health points a thrown survivor loses on landing, never taking them below 1.
     *                            0 turns it off
     */
    public record Catch(int betrayalCatchCount, double betrayalChance, int betrayalGlowSeconds, int slownessSeconds,
                        double swapChance, double launchDamage) {

        /** The most health a landing may cost, in health points. */
        public static final double MAX_LAUNCH_DAMAGE = 20.0D;

        /** The default catch. */
        public static final Catch DEFAULT = new Catch(2, 0.15D, 6, 4, 0.5D, 4.0D);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range
         */
        public Catch {
            atLeast("betrayalCatchCount", betrayalCatchCount, 1);
            between("betrayalChance", betrayalChance, 0.0D, 1.0D);
            atLeast("betrayalGlowSeconds", betrayalGlowSeconds, 1);
            atLeast("slownessSeconds", slownessSeconds, 0);
            between("swapChance", swapChance, 0.0D, 1.0D);
            between("launchDamage", launchDamage, 0.0D, MAX_LAUNCH_DAMAGE);
        }

        /**
         * Reads the catch.
         *
         * @param section the {@code creek.} part of the config
         * @return the catch
         */
        public static Catch read(ConfigSection section) {
            return section.orDefault("creek catch", () -> new Catch(
                    section.getInt("betrayalCatchCount", DEFAULT.betrayalCatchCount()),
                    section.getDouble("betrayalChance", DEFAULT.betrayalChance()),
                    section.getInt("betrayalGlowSeconds", DEFAULT.betrayalGlowSeconds()),
                    section.getInt("slownessSeconds", DEFAULT.slownessSeconds()),
                    section.getDouble("swapChance", DEFAULT.swapChance()),
                    section.getDouble("launchDamage", DEFAULT.launchDamage())), DEFAULT);
        }
    }
}
