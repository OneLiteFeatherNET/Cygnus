package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link OverlayTextureKeys} reproduces the {@code cygnus:} key conventions the
 * tunnel vision, glitch and blood renderers built by hand.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class OverlayTextureKeysTest {

    @Test
    @DisplayName("A flat table matches the tunnel vision's stage_<n> convention")
    void flatMatchesTunnelVisionConvention() {
        Key[] keys = OverlayTextureKeys.flat("gui/tunnel_vision/stage_", 3, OverlayTextureKeys.ONE_BASED);

        assertEquals(Key.key("cygnus", "gui/tunnel_vision/stage_1"), keys[0]);
        assertEquals(Key.key("cygnus", "gui/tunnel_vision/stage_2"), keys[1]);
        assertEquals(Key.key("cygnus", "gui/tunnel_vision/stage_3"), keys[2]);
    }

    @Test
    @DisplayName("A two-dimensional table matches the glitch's level_<level>_<frame> convention")
    void tableMatchesGlitchConvention() {
        Key[][] keys = OverlayTextureKeys.table(
                "gui/glitch/level_", 2, 2, OverlayTextureKeys.ONE_BASED, OverlayTextureKeys.ONE_BASED);

        assertEquals(Key.key("cygnus", "gui/glitch/level_1_1"), keys[0][0]);
        assertEquals(Key.key("cygnus", "gui/glitch/level_1_2"), keys[0][1]);
        assertEquals(Key.key("cygnus", "gui/glitch/level_2_1"), keys[1][0]);
        assertEquals(Key.key("cygnus", "gui/glitch/level_2_2"), keys[1][1]);
    }

    @Test
    @DisplayName("A three-dimensional table matches the blood's <direction>_<variant>_<frame> convention")
    void cubeMatchesBloodConvention() {
        String[] directions = {"left", "right"};

        Key[][][] keys = OverlayTextureKeys.cube(
                "gui/blood/", 2, 2, 2,
                index -> directions[index], OverlayTextureKeys.ONE_BASED, OverlayTextureKeys.ONE_BASED);

        assertEquals(Key.key("cygnus", "gui/blood/left_1_1"), keys[0][0][0]);
        assertEquals(Key.key("cygnus", "gui/blood/left_2_1"), keys[0][1][0]);
        assertEquals(Key.key("cygnus", "gui/blood/right_1_2"), keys[1][0][1]);
    }
}
