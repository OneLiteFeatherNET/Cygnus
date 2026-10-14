package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;

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
 * @param perSecond  points per second regained by a calm survivor, fear slows it down
 * @param shares     the share of the maximum health that comes back, one entry for every
 *                   {@value #SURVIVORS_PER_STEP} survivors, the last one for every bigger round
 * @param minMissing the points a survivor has to miss before the painkillers can be taken, so the
 *                   one chance is not spent on a scratch. At least 1
 * @param minSpeed   the share of {@code perSecond} a terrified survivor still regains, between 0 and
 *                   1, so fear slows the healing down but never stops it. At 1 fear plays no part
 * @author theEvilReaper
 * @version 1.2.0
 * @since 2.17.0
 */
public record RegenerationConfig(double perSecond, List<Double> shares, int minMissing, double minSpeed) {

    /** How many more survivors it takes to move on to the next share. */
    public static final int SURVIVORS_PER_STEP = GameConfig.SMALL_ROUND_SURVIVORS;

    /**
     * The default settings: 40 % back with 1 to 3 survivors, 30 % with 4 to 6, 25 % with 7 to 9 and
     * 20 % from 10 survivors on, at 4 points per second for a calm survivor and half of that for a
     * terrified one. The painkillers need a whole missing heart.
     */
    public static final RegenerationConfig DEFAULT = new RegenerationConfig(4.0D, List.of(0.40D, 0.30D, 0.25D, 0.20D), 10, 0.5D);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public RegenerationConfig {
        notNegative("perSecond", perSecond);
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("shares must not be empty");
        }
        for (double share : shares) {
            if (share < 0.0D || share > 1.0D) {
                throw new IllegalArgumentException("shares (" + shares + ") must all be between 0.0 and 1.0");
            }
        }
        atLeast("minMissing", minMissing, 1);
        between("minSpeed", minSpeed, 0.0D, 1.0D);
        shares = List.copyOf(shares);
    }

    /**
     * Reads the settings. The section carries the {@code regeneration.} prefix. Invalid values fall
     * back to the DEFAULT.
     *
     * @param section the {@code regeneration.} part of the config
     * @return the settings
     */
    public static RegenerationConfig read(ConfigSection section) {
        return section.orDefault("regeneration", () -> new RegenerationConfig(
                section.getDouble("perSecond", DEFAULT.perSecond()),
                readShares(section.getString("shares"), DEFAULT.shares()),
                section.getInt("minMissing", DEFAULT.minMissing()),
                section.getDouble("minSpeed", DEFAULT.minSpeed())
        ), DEFAULT);
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
     * Returns the points per second a survivor with this fear regains.
     * <p>
     * A calm survivor regains {@link #perSecond()}, a terrified one {@link #minSpeed()} of it, and
     * everything in between scales along.
     * </p>
     *
     * @param dread the survivor's fear, clamped to 0 to 1
     * @return the points per second
     */
    public double speedFor(double dread) {
        double calm = 1.0D - Math.clamp(dread, 0.0D, 1.0D);
        return this.perSecond * (this.minSpeed + (1.0D - this.minSpeed) * calm);
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
