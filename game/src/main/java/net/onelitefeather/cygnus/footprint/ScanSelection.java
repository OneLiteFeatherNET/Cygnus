package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Picks which recorded points a scan shows.
 * <p>
 * Points closer together than the spacing are shown once, so two survivors walking the same
 * corridor do not stack prints. A block holds only one print, so a second point in the same block is
 * skipped as well. If more points are left than a scan may show, every n-th one is
 * taken, so the shown prints still cover the whole track and not only its start.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class ScanSelection {

    /**
     * Selects the points to show.
     *
     * @param points     the candidate points
     * @param max        how many points may be shown at most
     * @param minSpacing how close two shown points may lie, in blocks
     * @return the positions to place prints at
     */
    static List<Pos> select(List<TrackPoint> points, int max, double minSpacing) {
        double spacingSquared = minSpacing * minSpacing;
        List<Pos> spaced = new ArrayList<>();
        Set<BlockVec> blocks = new HashSet<>();
        for (TrackPoint point : points.stream().sorted(Comparator.comparingLong(TrackPoint::timeMillis)).toList()) {
            Pos position = point.position();
            if (spaced.stream().anyMatch(other -> other.distanceSquared(position) < spacingSquared)) continue;
            // A block holds one print, a second point in it would only replace the first.
            if (!blocks.add(new BlockVec(position.blockX(), position.blockY(), position.blockZ()))) continue;
            spaced.add(position);
        }
        if (spaced.size() <= max) return spaced;
        List<Pos> thinned = new ArrayList<>(max);
        double stride = (double) spaced.size() / max;
        for (int i = 0; i < max; i++) {
            thinned.add(spaced.get((int) (i * stride)));
        }
        return thinned;
    }

    private ScanSelection() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
