package net.onelitefeather.cygnus.common.config;

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
        double fadeShare
) {

    /**
     * The start values from the design. They still need a playtest.
     */
    public static final FootprintConfig DEFAULT = new FootprintConfig(
            3.0D, 0.25D, 1000, 2000, 12,
            2.0D, 30,
            40, 5, 40, 15, 120,
            8.0D, 0.7D, 0.33D);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public FootprintConfig {
        positive("slenderStepBlocks", slenderStepBlocks);
        if (slenderChance <= 0.0D || slenderChance > 1.0D) {
            throw new IllegalArgumentException("slenderChance (" + slenderChance + ") must be above 0.0 and at most 1.0");
        }
        if (slenderDelayMinMillis < 0) {
            throw new IllegalArgumentException("slenderDelayMinMillis (" + slenderDelayMinMillis + ") must not be negative");
        }
        if (slenderDelayMaxMillis < slenderDelayMinMillis) {
            throw new IllegalArgumentException("slenderDelayMaxMillis (" + slenderDelayMaxMillis
                    + ") must be at least slenderDelayMinMillis (" + slenderDelayMinMillis + ")");
        }
        atLeastOne("slenderLifetimeSeconds", slenderLifetimeSeconds);
        positive("survivorSampleBlocks", survivorSampleBlocks);
        atLeastOne("survivorHistorySeconds", survivorHistorySeconds);
        atLeastOne("scanRadius", scanRadius);
        if (scanGapSeconds < 0 || scanGapSeconds >= survivorHistorySeconds) {
            throw new IllegalArgumentException("scanGapSeconds (" + scanGapSeconds
                    + ") must be at least 0 and below survivorHistorySeconds (" + survivorHistorySeconds + ")");
        }
        atLeastOne("scanMaxPrints", scanMaxPrints);
        atLeastOne("scanLifetimeSeconds", scanLifetimeSeconds);
        if (scanCooldownSeconds < 0) {
            throw new IllegalArgumentException("scanCooldownSeconds (" + scanCooldownSeconds + ") must not be negative");
        }
        if (teleportBlocks <= Math.max(slenderStepBlocks, survivorSampleBlocks)) {
            throw new IllegalArgumentException("teleportBlocks (" + teleportBlocks
                    + ") must be longer than slenderStepBlocks and survivorSampleBlocks");
        }
        if (minSpacing < 0.0D) {
            throw new IllegalArgumentException("minSpacing (" + minSpacing + ") must not be negative");
        }
        if (fadeShare < 0.0D || fadeShare > 1.0D) {
            throw new IllegalArgumentException("fadeShare (" + fadeShare + ") must be between 0.0 and 1.0");
        }
    }

    /**
     * Reads the footprint settings. The section carries the {@code footprint.} prefix.
     *
     * @param section the {@code footprint.} part of the config
     * @return the footprint settings
     * @throws IllegalArgumentException if a value is out of range
     */
    public static FootprintConfig read(ConfigSection section) {
        FootprintConfig defaults = DEFAULT;
        return new FootprintConfig(
                section.getDouble("slenderStepBlocks", defaults.slenderStepBlocks()),
                section.getDouble("slenderChance", defaults.slenderChance()),
                section.getInt("slenderDelayMinMillis", defaults.slenderDelayMinMillis()),
                section.getInt("slenderDelayMaxMillis", defaults.slenderDelayMaxMillis()),
                section.getInt("slenderLifetimeSeconds", defaults.slenderLifetimeSeconds()),
                section.getDouble("survivorSampleBlocks", defaults.survivorSampleBlocks()),
                section.getInt("survivorHistorySeconds", defaults.survivorHistorySeconds()),
                section.getInt("scanRadius", defaults.scanRadius()),
                section.getInt("scanGapSeconds", defaults.scanGapSeconds()),
                section.getInt("scanMaxPrints", defaults.scanMaxPrints()),
                section.getInt("scanLifetimeSeconds", defaults.scanLifetimeSeconds()),
                section.getInt("scanCooldownSeconds", defaults.scanCooldownSeconds()),
                section.getDouble("teleportBlocks", defaults.teleportBlocks()),
                section.getDouble("minSpacing", defaults.minSpacing()),
                section.getDouble("fadeShare", defaults.fadeShare())
        );
    }

    private static void positive(String name, double value) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " (" + value + ") must be above 0.0");
        }
    }

    private static void atLeastOne(String name, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " (" + value + ") must be at least 1");
        }
    }
}
