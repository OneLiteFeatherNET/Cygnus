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
    private record LoggingFeature(String name, boolean enabled, List<String> log) implements GameFeature {

        @Override
        public void registerListener(EventNode<Event> node) {
            node.addListener(Ping.class, _ -> this.log.add(this.name));
        }
    }

    @Test
    void testAnEnabledFeatureListensOnAChildNodeNamedAfterIt() {
        EventNode<Event> root = EventNode.all("root");
        List<String> log = new ArrayList<>();

        GameFeatures.register(root, List.of(new LoggingFeature("creek", true, log)));
        root.call(new Ping());

        assertEquals(List.of("creek"), log);
        assertEquals(1, root.findChildren("creek").size(), "the feature must get a node of its own, named after it");
    }

    @Test
    void testADisabledFeatureGetsNoNode() {
        EventNode<Event> root = EventNode.all("root");
        List<String> log = new ArrayList<>();

        GameFeatures.register(root, List.of(new LoggingFeature("creek", false, log)));
        root.call(new Ping());

        assertTrue(log.isEmpty(), "a disabled feature must not hear anything");
        assertTrue(root.getChildren().isEmpty(), "a disabled feature must not leave an empty node behind");
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
    void testTheDefaultNameIsTheSimpleClassName() {
        GameFeature feature = _ -> {
        };

        assertEquals(feature.getClass().getSimpleName(), feature.name());
        assertTrue(feature.enabled(), "a feature without a switch of its own is always on");
    }
}
