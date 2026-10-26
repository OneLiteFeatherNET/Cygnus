package net.onelitefeather.cygnus.common.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A page pool of a map: a named set of spots the pages of a round are placed on.
 *
 * <p>A map can have several pools, each in its own file in the {@code pages} folder (see
 * {@link PagePoolFiles}). Every round is played with one of them, picked by {@link #forRound(Path, int)},
 * so the pages do not show up in the same spots every round. The pool named {@link #DEFAULT_NAME} is
 * the fallback: it is played whenever no pool has enough spots for a round to start.</p>
 *
 * @param name  the name of the pool, which is also the name of its file without {@code .json}
 * @param pages the spots of the pool
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record PagePool(String name, Set<PageResource> pages) {

    /** The name of the pool that is played if no pool has enough spots. */
    public static final String DEFAULT_NAME = "default";

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePool.class);

    /**
     * Loads the pools of a map and picks the one a round is played with, see {@link #pick(Collection, int)}.
     * A broken pool file or an unreadable {@code pages} folder is logged and left out, so the round
     * still starts.
     *
     * @param mapFile  the map file the pools belong to
     * @param minSpots how many pages a round starts with; a pool with fewer spots is left out
     * @return the pool for the round
     */
    public static PagePool forRound(Path mapFile, int minSpots) {
        List<PagePool> pools;
        try {
            pools = PagePoolFiles.load(mapFile, (name, reason) -> LOGGER.warn("Skipped page pool {}: {}", name, reason));
        } catch (UncheckedIOException exception) {
            LOGGER.warn("Could not read the page pools, only the default pool is played: {}", exception.getMessage());
            pools = List.of();
        }
        return pick(pools, minSpots);
    }

    /**
     * Picks one of the given pools, each with the same chance. A pool with fewer than {@code minSpots}
     * spots is left out. If none is left, the default pool is picked, an empty one if it is missing.
     *
     * @param pools    the pools of the map
     * @param minSpots how many pages a round starts with
     * @return the picked pool
     */
    static PagePool pick(Collection<PagePool> pools, int minSpots) {
        List<PagePool> candidates = new ArrayList<>();
        for (PagePool pool : pools) {
            if (pool.pages().size() >= minSpots) {
                candidates.add(pool);
            } else {
                LOGGER.warn("Skipped page pool {}: {} spots, but a round starts with {} pages",
                        pool.name(), pool.pages().size(), minSpots);
            }
        }
        if (candidates.isEmpty()) {
            LOGGER.warn("No page pool has enough spots, the default pool is played");
            return pools.stream()
                    .filter(pool -> DEFAULT_NAME.equals(pool.name()))
                    .findFirst()
                    .orElseGet(() -> new PagePool(DEFAULT_NAME, Set.of()));
        }
        PagePool picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        LOGGER.info("Picked page pool {} with {} spots out of {} pools", picked.name(), picked.pages().size(),
                candidates.size());
        return picked;
    }
}
