package net.onelitefeather.cygnus.common.config;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * How survivors win back health with their painkillers, once per round.
 * <p>
 * Taking them heals over a few seconds, at most a share of the maximum health. The share shrinks
 * with the round: small rounds forgive more than big ones. The keys carry the
 * {@code regeneration.} prefix, for example {@code regeneration.shares=0.4,0.3,0.25,0.2}.
 * </p>
 *
 * @param perSecond points per second regained by a calm survivor, fear slows it down
 * @param shares    the share of the maximum health that comes back, one entry for every
 *                  {@value #SURVIVORS_PER_STEP} survivors, the last one for every bigger round
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.17.0
 */
public record RegenerationConfig(double perSecond, List<Double> shares) {

    /** How many more survivors it takes to move on to the next share. */
    public static final int SURVIVORS_PER_STEP = 3;

    /**
     * The default settings: 40 % back with 1 to 3 survivors, 30 % with 4 to 6, 25 % with 7 to 9 and
     * 20 % from 10 survivors on, at 4 points per second for a calm survivor.
     */
    public static final RegenerationConfig DEFAULT = new RegenerationConfig(4.0D, List.of(0.40D, 0.30D, 0.25D, 0.20D));

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public RegenerationConfig {
        if (perSecond < 0.0D) {
            throw new IllegalArgumentException("perSecond (" + perSecond + ") must not be negative");
        }
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("shares must not be empty");
        }
        for (double share : shares) {
            if (share < 0.0D || share > 1.0D) {
                throw new IllegalArgumentException("shares (" + shares + ") must all be between 0.0 and 1.0");
            }
        }
        shares = List.copyOf(shares);
    }

    /**
     * Reads the settings. The section carries the {@code regeneration.} prefix. Invalid values fall
     * back to the defaults.
     *
     * @param section the {@code regeneration.} part of the config
     * @return the settings
     */
    public static RegenerationConfig read(ConfigSection section) {
        RegenerationConfig defaults = DEFAULT;
        return section.orDefault("regeneration", () -> new RegenerationConfig(
                section.getDouble("perSecond", defaults.perSecond()),
                readShares(section.getString("shares"), defaults.shares())
        ), defaults);
    }

    /**
     * Returns the share of the maximum health that comes back in a round of this size.
     *
     * @param survivors the survivors at the start of the round
     * @return the share
     */
    public double shareFor(int survivors) {
        int step = Math.max(0, survivors - 1) / SURVIVORS_PER_STEP;
        return this.shares.get(Math.min(step, this.shares.size() - 1));
    }

    /**
     * Reads a comma separated list of shares.
     *
     * @throws NumberFormatException if an entry is no number, which falls back to the defaults
     */
    private static List<Double> readShares(@Nullable String value, List<Double> fallback) {
        if (value == null) return fallback;
        List<Double> shares = new ArrayList<>();
        for (String part : value.split(",")) {
            shares.add(Double.parseDouble(part.trim()));
        }
        return shares;
    }
}
