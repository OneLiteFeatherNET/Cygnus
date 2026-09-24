package net.onelitefeather.cygnus.creek.dread;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;

import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Rates the dread from the found pages, the elapsed time and whether a survivor is alone.
 * <p>
 * Pages and time are the same for everyone. Being alone is what makes the difference, so the
 * creek prefers survivors who leave the group.
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
     * Creates the rating.
     *
     * @param foundPages      supplies the number of found pages
     * @param maxPages        supplies the total number of pages
     * @param elapsedMillis   supplies the elapsed round time
     * @param gameTimeSeconds the maximum round length
     * @param config          the weights and the isolation radius
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
