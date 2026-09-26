package net.onelitefeather.cygnus.creek.dread;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;

import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Rates the dread from the pages found so far, how long the round has been going and whether a
 * survivor is on their own.
 * <p>
 * Pages and time are the same for everyone. Being alone is what sets a survivor apart, so the
 * creek goes for those who wander off from the group.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageProgressDread implements DreadSource {

    private final IntSupplier foundPages;
    private final IntSupplier maxPages;
    private final LongSupplier elapsedMillis;
    private final long roundMillis;
    private final CreekConfig config;

    /**
     * Sets up the rating.
     *
     * @param foundPages      supplies how many pages have been found
     * @param maxPages        supplies how many pages there are in total
     * @param elapsedMillis   supplies how long the round has been going
     * @param gameTimeSeconds how long a round lasts at most
     * @param config          the weights and how far apart counts as alone
     */
    public PageProgressDread(IntSupplier foundPages, IntSupplier maxPages, LongSupplier elapsedMillis,
                             int gameTimeSeconds, CreekConfig config) {
        this.foundPages = foundPages;
        this.maxPages = maxPages;
        this.elapsedMillis = elapsedMillis;
        this.roundMillis = gameTimeSeconds * 1000L;
        this.config = config;
    }

    @Override
    public double dreadOf(UUID survivor, Pos position, List<Pos> others) {
        double pages = share(this.foundPages.getAsInt(), this.maxPages.getAsInt());
        double time = share(this.elapsedMillis.getAsLong(), this.roundMillis);
        double isolated = this.isIsolated(position, others) ? 1.0D : 0.0D;
        double dread = this.config.dreadPageWeight() * pages
                + this.config.dreadTimeWeight() * time
                + this.config.dreadIsolationWeight() * isolated;
        return Math.clamp(dread, 0.0D, 1.0D);
    }

    private boolean isIsolated(Pos position, List<Pos> others) {
        double radius = this.config.isolationRadius();
        for (Pos other : others) {
            if (other.distance(position) <= radius) return false;
        }
        return true;
    }

    private static double share(double part, double whole) {
        if (whole <= 0.0D) return 0.0D;
        return Math.clamp(part / whole, 0.0D, 1.0D);
    }
}
