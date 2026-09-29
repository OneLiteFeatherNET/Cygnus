package net.onelitefeather.cygnus;

import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

/**
 * A self-contained part of the game that builds its own services and hooks them into the server.
 * <p>
 * {@link Cygnus} only puts the features together; whether a feature is switched on, and which of
 * its listeners that affects, is decided by the feature itself.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.15.0
 */
public interface GameFeature {

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
