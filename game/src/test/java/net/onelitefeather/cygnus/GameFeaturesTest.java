package net.onelitefeather.cygnus;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFeaturesTest {

    private record Ping() implements Event {
    }

    /**
     * A feature that writes its name into the shared log whenever it hears a {@link Ping}.
     */
    private static final class LoggingFeature implements GameFeature {

        private final EventNode<Event> node;
        private final boolean enabled;

        private LoggingFeature(String name, boolean enabled, List<String> log) {
            this.node = EventNode.all(name);
            this.enabled = enabled;
            this.node.addListener(Ping.class, _ -> log.add(name));
        }

        @Override
        public EventNode<Event> node() {
            return this.node;
        }

        @Override
        public boolean enabled() {
            return this.enabled;
        }
    }

    @Test
    void testAnEnabledFeatureHangsItsOwnNodeBelowTheParent() {
        EventNode<Event> root = EventNode.all("root");
        List<String> log = new ArrayList<>();

        GameFeatures.register(root, List.of(new LoggingFeature("creek", true, log)));
        root.call(new Ping());

        assertEquals(List.of("creek"), log);
        assertEquals(List.of("creek"), root.getChildren().stream().map(EventNode::getName).toList(),
                "the feature's own node must hang below the parent");
    }

    @Test
    void testADisabledFeatureIsNotHungIn() {
        EventNode<Event> root = EventNode.all("root");
        List<String> log = new ArrayList<>();

        GameFeatures.register(root, List.of(new LoggingFeature("creek", false, log)));
        root.call(new Ping());

        assertTrue(log.isEmpty(), "a disabled feature must not hear anything");
        assertTrue(root.getChildren().isEmpty(), "a disabled feature must not end up in the tree");
    }

    @Test
    void testFeaturesHearEventsAfterTheParentAndInTheirListOrder() {
        EventNode<Event> root = EventNode.all("root");
        List<String> log = new ArrayList<>();
        root.addListener(Ping.class, _ -> log.add("root"));

        GameFeatures.register(root, List.of(
                new LoggingFeature("first", true, log),
                new LoggingFeature("second", true, log),
                new LoggingFeature("third", true, log)));
        root.call(new Ping());

        assertEquals(List.of("root", "first", "second", "third"), log,
                "the features registered last on the global handler before, so their order must not change");
    }

    @Test
    void testAFeatureWithoutASwitchOfItsOwnIsAlwaysOn() {
        GameFeature feature = () -> EventNode.all("plain");

        assertTrue(feature.enabled());
    }
}
