package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.positive;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.share;

/**
 * Settings for the footprints both sides leave behind.
 * <p>
 * The slender leaves a print by chance while he is hidden, and only survivors see it. Survivors
 * leave tracks all the time, but those are only recorded. The slender reveals the recent ones
 * around him with his tracking item, which then needs a cooldown.
 * </p>
 *
 * @param slenderStepBlocks      how far the hidden slender walks between two rolls, in blocks
 * @param slenderChance          the chance of a print per roll, above 0 and at most 1
 * @param slenderDelayMinMillis  the shortest delay before a slender print appears
 * @param slenderDelayMaxMillis  the longest delay before a slender print appears
 * @param slenderLifetimeSeconds how long a slender print stays
 * @param survivorSampleBlocks   how far a survivor walks between two recorded points, in blocks
 * @param survivorHistorySeconds how long a recorded point is kept
 * @param scanRadius             how far around the slender the tracking item looks, in blocks
 * @param scanGapSeconds         how many of the newest seconds the tracking item leaves out
 * @param scanMaxPrints          how many prints one use shows at most
 * @param scanLifetimeSeconds    how long a revealed print stays
 * @param scanCooldownSeconds    how long the slender waits between two uses
 * @param teleportBlocks         a single move longer than this is a teleport and not walked distance
 * @param minSpacing             how close two revealed prints may lie, in blocks
 * @param fadeShare              the share of the lifetime at its end that shows the faded stage, from 0 to 1
 * @param scanStartDelaySeconds  how long the tracking item stays locked after the round starts. All
 *                               survivors start at the same spot, so an early scan would show where
 *                               each of them ran off to
 * @param scentPrints            how many prints point towards the nearest track when the radius holds none
 * @param scentSpreadDegrees     how far the scent may turn away from the true direction, to either side, from 0 to 180
 * @param scentCooldownSeconds   how long the slender waits after a use that found no tracks in the radius
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
public record FootprintConfig(
        double slenderStepBlocks,
        double slenderChance,
        int slenderDelayMinMillis,
        int slenderDelayMaxMillis,
        int slenderLifetimeSeconds,
        double survivorSampleBlocks,
        int survivorHistorySeconds,
        int scanRadius,
        int scanGapSeconds,
        int scanMaxPrints,
        int scanLifetimeSeconds,
        int scanCooldownSeconds,
        double teleportBlocks,
        double minSpacing,
        double fadeShare,
        int scanStartDelaySeconds,
        int scentPrints,
        int scentSpreadDegrees,
        int scentCooldownSeconds
) {

    /**
     * The start values from the design. They still need a playtest.
     */
    public static final FootprintConfig DEFAULT = new FootprintConfig(
            3.0D, 0.25D, 1000, 2000, 12,
            2.0D, 30,
            40, 5, 40, 15, 120,
            8.0D, 0.7D, 0.33D,
            150, 4, 15, 40);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public FootprintConfig {
        positive("slenderStepBlocks", slenderStepBlocks);
        share("slenderChance", slenderChance);
        notNegative("slenderDelayMinMillis", slenderDelayMinMillis);
        if (slenderDelayMaxMillis < slenderDelayMinMillis) {
            throw new IllegalArgumentException("slenderDelayMaxMillis (" + slenderDelayMaxMillis
                    + ") must be at least slenderDelayMinMillis (" + slenderDelayMinMillis + ")");
        }
        atLeast("slenderLifetimeSeconds", slenderLifetimeSeconds, 1);
        positive("survivorSampleBlocks", survivorSampleBlocks);
        atLeast("survivorHistorySeconds", survivorHistorySeconds, 1);
        atLeast("scanRadius", scanRadius, 1);
        if (scanGapSeconds < 0 || scanGapSeconds >= survivorHistorySeconds) {
            throw new IllegalArgumentException("scanGapSeconds (" + scanGapSeconds
                    + ") must be at least 0 and below survivorHistorySeconds (" + survivorHistorySeconds + ")");
        }
        atLeast("scanMaxPrints", scanMaxPrints, 1);
        atLeast("scanLifetimeSeconds", scanLifetimeSeconds, 1);
        notNegative("scanCooldownSeconds", scanCooldownSeconds);
        if (teleportBlocks <= Math.max(slenderStepBlocks, survivorSampleBlocks)) {
            throw new IllegalArgumentException("teleportBlocks (" + teleportBlocks
                    + ") must be longer than slenderStepBlocks and survivorSampleBlocks");
        }
        notNegative("minSpacing", minSpacing);
        between("fadeShare", fadeShare, 0.0D, 1.0D);
        notNegative("scanStartDelaySeconds", scanStartDelaySeconds);
        atLeast("scentPrints", scentPrints, 1);
        between("scentSpreadDegrees", scentSpreadDegrees, 0, 180);
        notNegative("scentCooldownSeconds", scentCooldownSeconds);
    }

    /**
     * Reads the footprint settings. The section carries the {@code footprint.} prefix.
     *
     * @param section the {@code footprint.} part of the config
     * @return the footprint settings
     */
    public static FootprintConfig read(ConfigSection section) {
        return section.orDefault("footprint", () -> new FootprintConfig(
                section.getDouble("slenderStepBlocks", DEFAULT.slenderStepBlocks()),
                section.getDouble("slenderChance", DEFAULT.slenderChance()),
                section.getInt("slenderDelayMinMillis", DEFAULT.slenderDelayMinMillis()),
                section.getInt("slenderDelayMaxMillis", DEFAULT.slenderDelayMaxMillis()),
                section.getInt("slenderLifetimeSeconds", DEFAULT.slenderLifetimeSeconds()),
                section.getDouble("survivorSampleBlocks", DEFAULT.survivorSampleBlocks()),
                section.getInt("survivorHistorySeconds", DEFAULT.survivorHistorySeconds()),
                section.getInt("scanRadius", DEFAULT.scanRadius()),
                section.getInt("scanGapSeconds", DEFAULT.scanGapSeconds()),
                section.getInt("scanMaxPrints", DEFAULT.scanMaxPrints()),
                section.getInt("scanLifetimeSeconds", DEFAULT.scanLifetimeSeconds()),
                section.getInt("scanCooldownSeconds", DEFAULT.scanCooldownSeconds()),
                section.getDouble("teleportBlocks", DEFAULT.teleportBlocks()),
                section.getDouble("minSpacing", DEFAULT.minSpacing()),
                section.getDouble("fadeShare", DEFAULT.fadeShare()),
                section.getInt("scanStartDelaySeconds", DEFAULT.scanStartDelaySeconds()),
                section.getInt("scentPrints", DEFAULT.scentPrints()),
                section.getInt("scentSpreadDegrees", DEFAULT.scentSpreadDegrees()),
                section.getInt("scentCooldownSeconds", DEFAULT.scentCooldownSeconds())
        ), DEFAULT);
    }
}
