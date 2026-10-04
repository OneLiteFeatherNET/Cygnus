package net.onelitefeather.cygnus.sanity;

import java.util.UUID;

/**
 * Hears about the discrete jumps in a survivor's fear.
 * <p>
 * A seam for tracing. Fear is continuous - it decays and creeps on its own - so only the events that
 * move it by a step are reported, each with where it came from. The service reads the value around
 * the jump only when an observer other than {@link #NONE} is installed.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface SanityObserver {

    /** The source of a jump caused by finding a page. */
    String SOURCE_PAGE = "page";
    /** The source of a jump caused by spotting a creek. */
    String SOURCE_SIGHTING = "sighting";
    /** The source of a jump caused by being caught by a creek. */
    String SOURCE_CAUGHT = "caught";
    /** The source of a jump caused by being picked out by a creek. */
    String SOURCE_SELECTED = "selected";
    /** The source of a jump caused by another survivor dying. */
    String SOURCE_DEATH = "death";
    /** The source of the steady rise while a creek stalks the survivor. */
    String SOURCE_STALK = "stalk";

    /**
     * Hears nothing.
     */
    SanityObserver NONE = (_, _, _, _) -> {
    };

    /**
     * A survivor's fear just moved.
     *
     * @param survivor the survivor
     * @param source   what moved it, one of the {@code SOURCE_} constants
     * @param before   the fear before, between 0 and 1
     * @param after    the fear after, between 0 and 1
     */
    void jumped(UUID survivor, String source, double before, double after);
}
