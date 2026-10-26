package net.onelitefeather.cygnus.common.page;

import java.util.Set;

/**
 * A named set of page spots of a map, one of which a round is played with (see {@link PagePoolDraw}).
 *
 * @param name  the name of the pool, {@link PagePoolFiles#DEFAULT_POOL} for the one in {@code pages.json}
 * @param pages the spots of the pool
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record PagePool(String name, Set<PageResource> pages) {
}
