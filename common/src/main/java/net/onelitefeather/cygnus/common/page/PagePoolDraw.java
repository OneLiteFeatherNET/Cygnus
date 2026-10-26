package net.onelitefeather.cygnus.common.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Draws the page pool a round is played with.
 *
 * <p>The candidates are the default pool from {@code pages.json} and every pool in the {@code pages}
 * folder (see {@link PagePoolFiles}). A pool with fewer spots than a round starts with is skipped.
 * One of the rest is drawn, each with the same chance. If none is left, the default pool is played,
 * just as it was before maps had pools.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PagePoolDraw {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePoolDraw.class);

    private PagePoolDraw() {
        // Nothing to do here
    }

    /**
     * A drawn pool.
     *
     * @param name  the name of the pool, {@link PagePoolFiles#DEFAULT_POOL} for the one in {@code pages.json}
     * @param pages the spots of the pool
     */
    public record Drawn(String name, Set<PageResource> pages) {
    }

    /**
     * Loads the pools of a map and draws one of them. A broken pool file or an unreadable
     * {@code pages} folder is logged and left out, so the round still starts.
     *
     * @param mapFile        the map file the pools belong to
     * @param defaultPages   the spots of the default pool
     * @param minActivePages how many pages a round starts with; a pool with fewer spots is skipped
     * @param random         the random source
     * @return the drawn pool, or the default pool if no pool has enough spots
     */
    public static Drawn drawFrom(Path mapFile, Set<PageResource> defaultPages, int minActivePages, RandomGenerator random) {
        Map<String, Set<PageResource>> pools;
        try {
            pools = PagePoolFiles.load(mapFile, (name, reason) ->
                    LOGGER.warn("Skipped page pool {} of {}: {}", name, mapFile, reason));
        } catch (UncheckedIOException exception) {
            LOGGER.warn("Could not read the page pools of {}, only the default pool is used: {}",
                    mapFile, exception.getMessage());
            pools = Map.of();
        }
        return draw(mapFile.toString(), defaultPages, pools, minActivePages, random);
    }

    /**
     * Draws one of the given pools.
     *
     * @param map            the map the pools belong to, only for the log
     * @param defaultPages   the spots of the default pool
     * @param pools          the other pools by name
     * @param minActivePages how many pages a round starts with; a pool with fewer spots is skipped
     * @param random         the random source
     * @return the drawn pool, or the default pool if no pool has enough spots
     */
    static Drawn draw(String map, Set<PageResource> defaultPages, Map<String, Set<PageResource>> pools,
                      int minActivePages, RandomGenerator random) {
        List<Drawn> candidates = new ArrayList<>(pools.size() + 1);
        addIfBigEnough(candidates, map, new Drawn(PagePoolFiles.DEFAULT_POOL, defaultPages), minActivePages);
        for (Map.Entry<String, Set<PageResource>> pool : pools.entrySet()) {
            addIfBigEnough(candidates, map, new Drawn(pool.getKey(), pool.getValue()), minActivePages);
        }
        if (candidates.isEmpty()) {
            LOGGER.warn("No page pool of {} has enough spots, the default pool is used", map);
            return new Drawn(PagePoolFiles.DEFAULT_POOL, defaultPages);
        }
        Drawn drawn = candidates.get(random.nextInt(candidates.size()));
        LOGGER.info("Drew page pool {} of {} with {} spots out of {} pools", drawn.name(), map,
                drawn.pages().size(), candidates.size());
        return drawn;
    }

    private static void addIfBigEnough(List<Drawn> candidates, String map, Drawn pool, int minActivePages) {
        if (pool.pages().size() < minActivePages) {
            LOGGER.warn("Skipped page pool {} of {}: {} spots, but a round starts with {} pages",
                    pool.name(), map, pool.pages().size(), minActivePages);
            return;
        }
        candidates.add(pool);
    }
}
