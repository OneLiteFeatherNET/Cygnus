package net.onelitefeather.cygnus.gaze;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the three things the resource pack's text shader depends on actually reach the wire.
 *
 * <p>This asserts on the serialized form rather than on the component, because the component is not
 * where this can break. The pack reads a glyph's colour; if Minestom ever stopped writing
 * {@code shadow_color}, vanilla would draw a shadow pass in a darkened colour and the pack would
 * see a second, wrong signal - with the component itself still looking perfectly correct in a test.
 * The same goes for the font: without it the glyph falls back and produces different geometry.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
@ExtendWith(MicrotusExtension.class)
class SignalWireProbeTest {

    @Test
    @DisplayName("Colour, font and the disabled shadow all reach the wire")
    void signalSurvivesSerialization(Env env) {
        String wire = serialize(3);

        assertTrue(wire.contains("#FE0004"), "level 3 has to arrive as base + 4: " + wire);
        assertTrue(wire.contains("shadow_color"),
                "without it vanilla draws a shadow pass in a darkened colour, a second wrong signal");
        assertTrue(wire.contains("cygnus:glitch"),
                "without the font the glyph falls back and produces different geometry");
    }

    @Test
    @DisplayName("Every level lands on its own colour, the strongest included")
    void everyLevelHasItsOwnColour(Env env) {
        assertTrue(serialize(SlenderGaze.NONE).contains("#FE0000"));
        assertTrue(serialize(0).contains("#FE0001"));
        assertTrue(serialize(1).contains("#FE0002"));
        assertTrue(serialize(2).contains("#FE0003"));
        assertTrue(serialize(3).contains("#FE0004"), "the strongest level must not wrap");
    }

    /**
     * Serializes a level's signal component and renders the bytes readable.
     *
     * @param level the level to encode
     * @return the wire form, with non-printable bytes replaced by dots
     */
    private static String serialize(int level) {
        byte[] bytes = NetworkBuffer.makeArray(
                NetworkBuffer.COMPONENT, BossBarGazeSignal.signalFor(level));
        StringBuilder readable = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            readable.append(b >= 32 && b < 127 ? (char) b : '.');
        }
        return readable.toString();
    }
}
