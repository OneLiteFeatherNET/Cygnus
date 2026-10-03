package net.onelitefeather.cygnus;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

import java.util.List;

/**
 * Hooks the {@link GameFeature}s into the event tree.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
public final class GameFeatures {

    private GameFeatures() {
    }

    /**
     * Gives every enabled feature a node of its own, named after it, below the given parent.
     * <p>
     * Minestom calls the parent's listeners first and the children in the order they were added,
     * so the features hear an event after the parent and in the order of the list.
     * </p>
     *
     * @param parent   the node to add the feature nodes to
     * @param features the features to register
     */
    public static void register(EventNode<Event> parent, List<GameFeature> features) {
        for (GameFeature feature : features) {
            if (!feature.enabled()) continue;
            EventNode<Event> node = EventNode.all(feature.name());
            feature.registerListener(node);
            parent.addChild(node);
        }
    }
}
