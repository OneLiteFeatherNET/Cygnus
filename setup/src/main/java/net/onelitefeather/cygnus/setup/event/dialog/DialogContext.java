package net.onelitefeather.cygnus.setup.event.dialog;

import net.minestom.server.coordinate.Point;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.page.PageResource;

/**
 * Represents the context for a dialog event which contains all necessary data
 * to open the correct dialog for a specific {@link DialogTarget}.
 *
 * @author Joltra
 * @version 1.6.0
 * @see DialogTarget
 * @since 0.1.0
 */
public sealed interface DialogContext permits
        DialogContext.NameContext,
        DialogContext.PositionContent,
        DialogContext.AuthorAmount,
        DialogContext.PageContent,
        DialogContext.AtmosphereContext,
        DialogContext.CreekPauseContext {

    /**
     * Specific context for the update or deletion of a name.
     *
     * @param name of the map
     */
    record NameContext(String name) implements DialogContext {

    }

    /**
     * Specific context to delete a position from the map.
     *
     * @param point to which should be deleted
     */
    record PositionContent(Point point) implements DialogContext {

    }

    /**
     * Specific context to indicate how many authors a map should have.
     *
     * @param amount of author
     */
    record AuthorAmount(float amount) implements DialogContext {

    }

    /**
     * Context implementation to bind a {@link PageResource} to a dialog.
     *
     * @param resource to bind
     */
    record PageContent(PageResource resource) implements DialogContext {

    }

    /**
     * Carries the atmosphere a builder is currently editing from one dialog of the chain to the
     * next, so the value dialog can pre-fill its sliders with what the preset step produced.
     *
     * @param atmosphere the atmosphere being edited
     */
    record AtmosphereContext(MapAtmosphere atmosphere) implements DialogContext {

    }


    /**
     * Carries the creek route whose pauses are edited, so the dialog can pre-fill its sliders.
     *
     * @param route       the route's name
     * @param startMillis the current pause at the start, in milliseconds
     * @param endMillis   the current pause at the end, in milliseconds
     */
    record CreekPauseContext(String route, int startMillis, int endMillis) implements DialogContext {

    }
}
