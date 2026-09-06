package net.onelitefeather.cygnus.overlay;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.GameFeatures;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class OverlayModuleTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(OverlayProperties.ENABLED_PROPERTY);
    }

    private static OverlayModule module() {
        return new OverlayModule(GameConfig.Glitch.DEFAULT, GameConfig.PageGlitch.DEFAULT, TeamService.of(), new StaminaService());
    }

    @Test
    @DisplayName("The module is on unless the overlays are switched off")
    void enabledByDefault() {
        assertTrue(module().enabled());
    }

    @Test
    @DisplayName("Switching the overlays off switches the module off")
    void followsOverlaySwitch() {
        System.setProperty(OverlayProperties.ENABLED_PROPERTY, "false");
        assertFalse(module().enabled());
    }

    @Test
    @DisplayName("Every effect gets its own node below the module")
    void nestsEffects() {
        EventNode<Event> root = EventNode.all("root");

        GameFeatures.register(root, List.of(module()));

        EventNode<Event> moduleNode = root.getChildren().iterator().next();
        Set<String> effects = moduleNode.getChildren().stream().map(EventNode::getName).collect(Collectors.toSet());
        assertEquals("overlay", moduleNode.getName());
        assertEquals(Set.of("slender-gaze", "page-glitch", "blood-splatter", "tunnel-vision"), effects);
    }
}
