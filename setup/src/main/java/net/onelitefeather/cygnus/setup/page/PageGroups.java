package net.onelitefeather.cygnus.setup.page;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.page.PageResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the groups of page spots a map builder placed close together, so the setup can show how
 * much of a pool sits in one part of the map.
 * <p>
 * Two spots within {@link #LINK_DISTANCE} of each other belong to the same group, and so does every
 * spot linked to them over a chain of such neighbours. A group therefore grows along a house or a
 * path, but a spot further away stands on its own however many spots there are around it.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PageGroups {

    /** How close two spots have to be to belong to the same group, in blocks. */
    static final double LINK_DISTANCE = 10.0D;

    /** Above this share of all spots a group draws too many pages to one place. */
    static final double CROWDED_SHARE = 0.25D;

    private PageGroups() {
    }

    /**
     * Groups the given spots.
     *
     * @param spots the spots of a pool
     * @return the groups, the largest first, every spot in exactly one of them
     */
    public static List<Group> find(Collection<PageResource> spots) {
        List<PageResource> all = List.copyOf(spots);
        int[] parents = new int[all.size()];
        for (int index = 0; index < parents.length; index++) {
            parents[index] = index;
        }
        double link = LINK_DISTANCE * LINK_DISTANCE;
        for (int first = 0; first < all.size(); first++) {
            for (int second = first + 1; second < all.size(); second++) {
                if (all.get(first).position().distanceSquared(all.get(second).position()) <= link) {
                    parents[root(parents, first)] = root(parents, second);
                }
            }
        }
        Map<Integer, List<PageResource>> members = new LinkedHashMap<>();
        for (int index = 0; index < all.size(); index++) {
            members.computeIfAbsent(root(parents, index), _ -> new ArrayList<>()).add(all.get(index));
        }
        return members.values().stream()
                .map(group -> new Group(List.copyOf(group), (double) group.size() / all.size()))
                .sorted(Comparator.comparingInt((Group group) -> group.spots().size()).reversed())
                .toList();
    }

    private static int root(int[] parents, int index) {
        int root = index;
        while (parents[root] != root) {
            root = parents[root];
        }
        // Point every spot on the way straight at the root, so later lookups stay short
        while (parents[index] != root) {
            int next = parents[index];
            parents[index] = root;
            index = next;
        }
        return root;
    }

    /**
     * Spots that lie close together.
     *
     * @param spots the spots of the group
     * @param share how much of all spots the group holds, between {@code 0} and {@code 1}
     */
    public record Group(List<PageResource> spots, double share) {

        /**
         * Returns the middle of the group's spots.
         *
         * @return the average position of the spots
         */
        public Point centre() {
            Vec sum = Vec.ZERO;
            for (PageResource spot : this.spots) {
                sum = sum.add(spot.position());
            }
            return sum.div(this.spots.size());
        }

        /**
         * Tells whether the group holds too many of the spots, see {@link #CROWDED_SHARE}. A lone spot
         * never does, it is no group to thin out.
         *
         * @return {@code true} if the group should be thinned out
         */
        public boolean crowded() {
            return this.spots.size() > 1 && this.share > CROWDED_SHARE;
        }
    }
}
