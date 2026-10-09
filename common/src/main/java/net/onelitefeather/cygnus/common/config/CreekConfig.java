package net.onelitefeather.cygnus.common.config;

/**
 * Settings for the creek, the second figure next to the slender.
 * <p>
 * All numbers that control the creek's behavior live here, so they can be tuned in a playtest
 * without changing code. They are sorted into groups, but the keys in {@code config.properties}
 * stay flat, for example {@code creek.stalkMinDistance}. Each group rejects values that break it,
 * and this record rejects the combinations that span groups, for example a hiding spot inside the
 * view cone.
 * </p>
 *
 * @param enabled                whether the creek takes part in a round
 * @param activeWithLastSurvivor whether the creek keeps going when only one survivor is left
 * @param routeLinkDistance      how close two route ends have to be to count as linked, in blocks
 * @param personalSpace          how close a survivor may come outside a hunt, in blocks
 * @param stuckMillis            how long the creek may make no progress before taking a shortcut
 * @param sight                  when a survivor notices the creek
 * @param wander                 how the creek moves while patrolling
 * @param stalk                  how the creek follows a scared survivor
 * @param hunt                   how the creek chases a survivor
 * @param vanish                 how long the creek stays away and where it comes back
 * @param catching               what a catch does
 * @author theEvilReaper
 * @version 2.0.0
 * @since 2.15.0
 */
public record CreekConfig(
        boolean enabled,
        boolean activeWithLastSurvivor,
        double routeLinkDistance,
        int personalSpace,
        int stuckMillis,
        Sight sight,
        Wander wander,
        Stalk stalk,
        Hunt hunt,
        Vanish vanish,
        Catch catching
) {

    /**
     * The default settings: enabled, and tuned for a round of about fifteen minutes.
     */
    public static final CreekConfig DEFAULT = new CreekConfig(
            true, true, 3.0D, 15, 3000,
            Sight.DEFAULT, Wander.DEFAULT, Stalk.DEFAULT, Hunt.DEFAULT, Vanish.DEFAULT, Catch.DEFAULT
    );

    /**
     * Checks the top-level values and the combinations that span groups.
     *
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other
     */
    public CreekConfig {
        positive("routeLinkDistance", routeLinkDistance);
        atLeast("personalSpace", personalSpace, 1);
        atLeast("stuckMillis", stuckMillis, 1);
        below("stalkThreshold", stalk.threshold(), "huntThreshold", hunt.threshold());
        below("personalSpace", personalSpace, "stalkMinDistance", stalk.minDistance());
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
        CreekConfig fallback = new CreekConfig(enabled, activeWithLastSurvivor, DEFAULT.routeLinkDistance(),
                DEFAULT.personalSpace(), DEFAULT.stuckMillis(), Sight.DEFAULT, Wander.DEFAULT, Stalk.DEFAULT,
                Hunt.DEFAULT, Vanish.DEFAULT, Catch.DEFAULT);
        return section.orDefault("creek", () -> new CreekConfig(
                enabled,
                activeWithLastSurvivor,
                section.getDouble("routeLinkDistance", DEFAULT.routeLinkDistance()),
                section.getInt("personalSpace", DEFAULT.personalSpace()),
                section.getInt("stuckMillis", DEFAULT.stuckMillis()),
                sight, wander, stalk, hunt, vanish, catching), fallback);
    }

    /**
     * When a survivor notices the creek.
     *
     * @param range     how far away a survivor can notice the creek, in blocks (key {@code sightRange})
     * @param viewAngle the maximum angle from a survivor's view center at which the creek counts as
     *                  seen, in degrees (key {@code sightViewAngle})
     */
    public record Sight(int range, int viewAngle) {

        /** The default sight. */
        public static final Sight DEFAULT = new Sight(48, 35);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range
         */
        public Sight {
            atLeast("sightRange", range, 1);
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
                    section.getInt("sightRange", DEFAULT.range()),
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
     * @param minDistance  minimum distance to the stalked survivor, in blocks (key {@code stalkMinDistance})
     * @param maxDistance  maximum distance to the stalked survivor, in blocks (key {@code stalkMaxDistance})
     * @param minAngle     minimum angle from the stalked survivor's view direction, in degrees (key {@code stalkMinAngle})
     * @param maxAngle     maximum angle from the stalked survivor's view direction, in degrees (key {@code stalkMaxAngle})
     * @param revealMillis how long the creek stays visible before it teleports away (key {@code stalkRevealMillis})
     * @param minSeconds   minimum length of a stalk (key {@code stalkMinSeconds})
     * @param maxSeconds   maximum length of a stalk (key {@code stalkMaxSeconds})
     */
    public record Stalk(double threshold, int minDistance, int maxDistance, int minAngle, int maxAngle,
                        int revealMillis, int minSeconds, int maxSeconds) {

        /** The default stalk. */
        public static final Stalk DEFAULT = new Stalk(0.25D, 20, 35, 40, 70, 700, 45, 90);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range or two values contradict each other
         */
        public Stalk {
            positive("stalkThreshold", threshold);
            below("stalkMinDistance", minDistance, "stalkMaxDistance", maxDistance);
            below("stalkMinAngle", minAngle, "stalkMaxAngle", maxAngle);
            between("stalkMaxAngle", maxAngle, 1, 180);
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
                    section.getInt("stalkMinDistance", DEFAULT.minDistance()),
                    section.getInt("stalkMaxDistance", DEFAULT.maxDistance()),
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
     * @param respawnMinDistance minimum distance to every survivor when reappearing, in blocks (key {@code respawnMinDistance})
     */
    public record Vanish(int minSeconds, int maxSeconds, int respawnMinDistance) {

        /** The default vanish. */
        public static final Vanish DEFAULT = new Vanish(20, 40, 30);

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if a value is out of range or two values contradict each other
         */
        public Vanish {
            atLeast("vanishMinSeconds", minSeconds, 0);
            notAbove("vanishMinSeconds", minSeconds, "vanishMaxSeconds", maxSeconds);
            atLeast("respawnMinDistance", respawnMinDistance, 1);
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
                    section.getInt("vanishMaxSeconds", DEFAULT.maxSeconds()),
                    section.getInt("respawnMinDistance", DEFAULT.respawnMinDistance())), DEFAULT);
        }
    }

    /**
     * What a catch does.
     *
     * @param betrayalCatchCount  from this catch on, a survivor is always revealed to the slender
     * @param betrayalChance      chance of a reveal on earlier catches
     * @param betrayalGlowSeconds how long a revealed survivor glows for the slender
     * @param slownessSeconds     how long a caught survivor is slowed
     * @param launchHeight        how high a caught survivor is thrown into the air, in blocks. 0 turns the launch off
     * @param swapChance          chance that a catch swaps two survivors instead of throwing the caught one.
     *                            0 always throws, 1 always swaps
     * @param launchDamage        health points a thrown survivor loses on landing, never taking them below 1.
     *                            0 turns it off
     */
    public record Catch(int betrayalCatchCount, double betrayalChance, int betrayalGlowSeconds, int slownessSeconds,
                        double launchHeight, double swapChance, double launchDamage) {

        /** The highest launch a catch may be set to, in blocks. */
        public static final double MAX_LAUNCH_HEIGHT = 20.0D;

        /** The most health a landing may cost, in health points. */
        public static final double MAX_LAUNCH_DAMAGE = 20.0D;

        /** The default catch. */
        public static final Catch DEFAULT = new Catch(2, 0.15D, 6, 4, 5.0D, 0.5D, 4.0D);

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
            between("launchHeight", launchHeight, 0.0D, MAX_LAUNCH_HEIGHT);
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
                    section.getDouble("launchHeight", DEFAULT.launchHeight()),
                    section.getDouble("swapChance", DEFAULT.swapChance()),
                    section.getDouble("launchDamage", DEFAULT.launchDamage())), DEFAULT);
        }
    }

    /**
     * Rejects a value below a minimum.
     *
     * @param name    the key name for the message
     * @param value   the value
     * @param minimum the smallest allowed value
     */
    private static void atLeast(String name, double value, double minimum) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " (" + value + ") must be at least " + minimum);
        }
    }

    /**
     * Rejects a value of 0 or below.
     *
     * @param name  the key name for the message
     * @param value the value
     */
    private static void positive(String name, double value) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " (" + value + ") must be above 0");
        }
    }

    /**
     * Rejects a value outside a range.
     *
     * @param name    the key name for the message
     * @param value   the value
     * @param minimum the smallest allowed value
     * @param maximum the largest allowed value
     */
    private static void between(String name, double value, double minimum, double maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " (" + value + ") must be between " + minimum + " and " + maximum);
        }
    }

    /**
     * Rejects a lower value that is not strictly below the upper one.
     *
     * @param lowerName the lower key name for the message
     * @param lower     the lower value
     * @param upperName the upper key name for the message
     * @param upper     the upper value
     */
    private static void below(String lowerName, double lower, String upperName, double upper) {
        if (lower >= upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + lower + ") must be below " + upperName + " (" + upper + ")");
        }
    }

    /**
     * Rejects a lower value above the upper one.
     *
     * @param lowerName the lower key name for the message
     * @param lower     the lower value
     * @param upperName the upper key name for the message
     * @param upper     the upper value
     */
    private static void notAbove(String lowerName, double lower, String upperName, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + lower + ") must not be above " + upperName + " (" + upper + ")");
        }
    }
}
