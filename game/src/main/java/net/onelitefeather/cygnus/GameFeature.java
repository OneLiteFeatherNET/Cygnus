package net.onelitefeather.cygnus;

import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

/**
 * A self-contained part of the game that builds its own services and hooks them into the server.
 * <p>
 * {@link Cygnus} only puts the features together; whether a feature is switched on is decided by
 * the feature itself through {@link #enabled()}. Every enabled feature listens on a node of its
 * own, see {@link GameFeatures#register}.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.15.0
 */
public interface GameFeature {

    /**
     * Returns the name of the feature, used for the event node it listens on.
     *
     * @return the name, the simple class name by default
     */
    default String name() {
        return getClass().getSimpleName();
    }

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
     * Registers the listeners of the feature.
     *
     * @param node the node to register on
     */
    void registerListener(EventNode<Event> node);

    /**
     * Registers the commands of the feature. Does nothing by default.
     *
     * @param manager the manager to register on
     */
    default void registerCommands(CommandManager manager) {
    }
}
