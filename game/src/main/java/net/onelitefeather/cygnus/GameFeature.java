package net.onelitefeather.cygnus;

import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

/**
 * A self-contained part of the game that builds its own services and hooks them into the server.
 * <p>
 * Every feature owns an event node and puts its listeners on it while it is built, so it is
 * complete on its own. {@link Cygnus} only hangs the nodes of the enabled features into the
 * global handler, see {@link GameFeatures#register}.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.15.0
 */
public interface GameFeature {

    /**
     * Returns the node the feature listens on, with all its listeners already in place.
     *
     * @return the feature's own node
     */
    EventNode<Event> node();

    /**
     * Returns whether the feature takes part in the game. A disabled feature gets no event node,
     * its commands are still registered.
     *
     * @return {@code true} by default
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Registers the commands of the feature. Does nothing by default.
     *
     * @param manager the manager to register on
     */
    default void registerCommands(CommandManager manager) {
    }
}
