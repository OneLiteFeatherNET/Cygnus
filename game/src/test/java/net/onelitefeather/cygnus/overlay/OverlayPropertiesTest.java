package net.onelitefeather.cygnus.overlay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the switch that decides whether the full-screen overlays run.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class OverlayPropertiesTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(OverlayProperties.ENABLED_PROPERTY);
    }

    @Test
    @DisplayName("The overlays run unless somebody says otherwise")
    void enabledByDefault() {
        assertTrue(OverlayProperties.enabled(), "the effects are part of the game, not an extra");
    }

    @Test
    @DisplayName("They can be switched off")
    void canBeSwitchedOff() {
        System.setProperty(OverlayProperties.ENABLED_PROPERTY, "false");
        assertFalse(OverlayProperties.enabled());
    }

    @Test
    @DisplayName("Switching them on explicitly works too")
    void canBeSwitchedOn() {
        System.setProperty(OverlayProperties.ENABLED_PROPERTY, "true");
        assertTrue(OverlayProperties.enabled());
    }

    @Test
    @DisplayName("Anything unreadable leaves them on rather than silently off")
    void nonsenseLeavesThemOn() {
        System.setProperty(OverlayProperties.ENABLED_PROPERTY, "perhaps");
        assertTrue(OverlayProperties.enabled(), "a typo must not take the effects out of the game");
    }
}
