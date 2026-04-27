package net.onelitefeather.cygnus.setup.event.dialog;

import net.minestom.server.coordinate.Pos;

/**
 * Represents the context for a dialog event which contains all necessary data
 * to open the correct dialog for a specific {@link DialogTarget}.
 *
 * @author Joltra
 * @version 1.0.0
 * @see DialogTarget
 * @since 0.1.0
 */
public sealed interface DialogContext permits DialogContext.NameContext, DialogContext.PositionContext {

    /**
     * Specific context for the update or deletion of a name.
     *
     * @param name of the map
     */
    record NameContext(String name) implements DialogContext {

    }

    record PositionContext(Pos pos) implements DialogContext {

    }
}
