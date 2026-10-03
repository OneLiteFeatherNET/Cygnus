package net.onelitefeather.cygnus.overlay;

import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class OverlayModuleTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(OverlayProperties.ENABLED_PROPERTY);
    }

    private static OverlayModule module() {
        return new OverlayModule(GameConfig.Glitch.DEFAULT, TeamService.of(), new StaminaService());
    }

    @Test
    @DisplayName("The module is on unless the overlays are switched off")
    void enabledByDefault() {
        assertTrue(module().enabled());
    }

    @Test
    @DisplayName("Switching the overlays off switches the whole module off")
    void followsTheOverlaySwitch() {
        System.setProperty(OverlayProperties.ENABLED_PROPERTY, "false");
        assertFalse(module().enabled());
    }
}
