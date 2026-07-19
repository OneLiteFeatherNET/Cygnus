package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.event.SlenderVisibilityChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class SlenderVisibilityChangeListenerTest {

    private static Instance instance;
    private static Player player;
    private static Player viewer;

    private SlenderVisibilityChangeListener listener;

    @BeforeAll
    static void init(@NotNull Env env) {
        instance = env.createFlatInstance();
        player = env.createPlayer(instance);
        viewer = env.createPlayer(instance);
    }

    @AfterAll
    static void cleanup(@NotNull Env env) {
        env.destroyInstance(instance, true);
    }

    @BeforeEach
    void setUp() {
        this.listener = new SlenderVisibilityChangeListener();
    }

    @Test
    void testSlenderHidden() {
        SlenderVisibilityChangeEvent event = new SlenderVisibilityChangeEvent(player, true);
        listener.accept(event);
        
        assertFalse(player.isViewer(viewer), "Slender should be hidden to viewers");
    }

    @Test
    void testSlenderVisible() {
        SlenderVisibilityChangeEvent event = new SlenderVisibilityChangeEvent(player, false);
        listener.accept(event);
        
        assertTrue(player.isViewer(viewer), "Slender should be visible to viewers");
    }
}
