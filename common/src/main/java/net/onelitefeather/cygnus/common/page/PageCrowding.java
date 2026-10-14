package net.onelitefeather.cygnus.common.page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Rates how crowded each page spot of a map is.
 * <p>
 * A part of a map with many spots would draw pages again and again, so the game hands crowded spots
 * out less often, and the setup marks them so a map builder sees where to thin them out. Both read
 * the same weights from here.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.18.0
 */
public final class PageCrowding {

    /** How far around a spot other spots count as crowding it, in blocks. */
    public static final double CROWD_RADIUS = 30.0D;

    /** The least share of an even turn a spot in a crowded part of the map gets. */
    public static final double LEAST_WEIGHT = 0.5D;

    /** The most share of an even turn a spot on its own gets, so no single tree draws the pages. */
    public static final double MOST_WEIGHT = 2.0D;

    /** Below this weight a spot has clearly more neighbours than the map's average, a third or more. */
    static final double CROWDED_BELOW = 0.75D;

    private PageCrowding() {
    }

    /**
     * Weighs every spot by how crowded its part of the map is: a spot with fewer others within
     * {@link #CROWD_RADIUS} than usual weighs more, one with more weighs less. The weights stay
     * between {@link #LEAST_WEIGHT} and {@link #MOST_WEIGHT}.
     *
     * @param spots the spots of the map
     * @return the weight of each spot, {@code 1} for a spot as crowded as the average one
     */
    public static Map<PageResource, Double> weights(Collection<PageResource> spots) {
        Map<PageResource, Integer> crowds = new HashMap<>();
        for (PageResource spot : spots) {
            int crowd = 0;
            for (PageResource other : spots) {
                if (spot.position().distance(other.position()) <= CROWD_RADIUS) crowd++;
            }
            crowds.put(spot, crowd);
        }
        double average = crowds.values().stream().mapToInt(Integer::intValue).average().orElse(1.0D);
        Map<PageResource, Double> weights = new HashMap<>();
        crowds.forEach((spot, crowd) -> weights.put(spot, Math.clamp(average / crowd, LEAST_WEIGHT, MOST_WEIGHT)));
        return weights;
    }

    /**
     * Tells whether a spot with this weight sits in a clearly crowded part of the map.
     *
     * @param weight the spot's weight from {@link #weights(Collection)}
     * @return {@code true} if the spot has a third more neighbours than the map's average or more
     */
    public static boolean isCrowded(double weight) {
        return weight < CROWDED_BELOW;
    }
}
