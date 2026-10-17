package net.onelitefeather.cygnus.common.config;

/**
 * The range checks the config records share. Each one throws an {@link IllegalArgumentException}
 * naming the key, which {@link ConfigSection#orDefault} turns into a warning and the group's
 * defaults. The settings a map sets for itself use them as well.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class ConfigChecks {

    private ConfigChecks() {
    }

    /**
     * Rejects a value below a minimum.
     *
     * @param name    the key name for the message
     * @param value   the value
     * @param minimum the smallest allowed value
     */
    public static void atLeast(String name, double value, double minimum) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " (" + format(value) + ") must be at least " + format(minimum));
        }
    }

    /**
     * Rejects a value below 0.
     *
     * @param name  the key name for the message
     * @param value the value
     */
    public static void notNegative(String name, double value) {
        if (value < 0.0D) {
            throw new IllegalArgumentException(name + " (" + format(value) + ") must not be negative");
        }
    }

    /**
     * Rejects a value of 0 or below.
     *
     * @param name  the key name for the message
     * @param value the value
     */
    public static void positive(String name, double value) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " (" + format(value) + ") must be above 0");
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
    public static void between(String name, double value, double minimum, double maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " (" + format(value) + ") must be between " + format(minimum) + " and " + format(maximum));
        }
    }

    /**
     * Rejects a share that is not above 0 and at most 1, for values where 0 would switch the effect off.
     *
     * @param name  the key name for the message
     * @param value the value
     */
    public static void share(String name, double value) {
        if (value <= 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " (" + format(value) + ") must be above 0 and at most 1");
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
    public static void below(String lowerName, double lower, String upperName, double upper) {
        if (lower >= upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + format(lower) + ") must be below " + upperName + " (" + format(upper) + ")");
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
    public static void notAbove(String lowerName, double lower, String upperName, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(
                    lowerName + " (" + format(lower) + ") must not be above " + upperName + " (" + format(upper) + ")");
        }
    }

    /**
     * Prints whole numbers without a fraction, so an int key reads {@code 5}, not {@code 5.0}.
     */
    private static String format(double value) {
        return value == Math.rint(value) && !Double.isInfinite(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
