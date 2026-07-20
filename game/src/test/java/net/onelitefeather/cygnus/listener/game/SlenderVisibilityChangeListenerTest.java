package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.event.SlenderVisibilityChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class SlenderVisibilityChangeListenerTest {

    @Test
    void testSlenderHidden(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player viewer = env.createPlayer(instance);

        // Register the listener to the test environment's EventNode
        env.process().eventHandler().addListener(SlenderVisibilityChangeEvent.class, new SlenderVisibilityChangeListener());

        // Fire the event via Minestom's event handler
        SlenderVisibilityChangeEvent event = new SlenderVisibilityChangeEvent(slender, true);
        env.process().eventHandler().call(event);
        
        assertFalse(slender.isViewer(viewer), "Slender should be hidden to viewers");
        
        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderVisible(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player viewer = env.createPlayer(instance);

        // Register the listener to the test environment's EventNode
        env.process().eventHandler().addListener(SlenderVisibilityChangeEvent.class, new SlenderVisibilityChangeListener());

        // Set up the state first as hidden, to test visibility toggle
        slender.updateViewableRule(v -> false);
        assertFalse(slender.isViewer(viewer));

        // Fire the event via Minestom's event handler
        SlenderVisibilityChangeEvent event = new SlenderVisibilityChangeEvent(slender, false);
        env.process().eventHandler().call(event);
        
        assertTrue(slender.isViewer(viewer), "Slender should be visible to viewers");
        
        env.destroyInstance(instance, true);
    }
}
