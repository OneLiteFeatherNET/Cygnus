package net.onelitefeather.cygnus.common.dimension;

import net.minestom.server.color.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the atmosphere the lobby is given: the map's own, pulled back towards the open end.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class BlendedAtmosphereTest {

    /** The open end a lobby is pulled towards. */
    private static final DimensionAtmosphere OPEN = new MapAtmosphere(
            new Color(100, 100, 100),
            new Color(200, 200, 200),
            new Color(150, 150, 150),
            new Color(10, 10, 10),
            1.0f, 20f, 200f, 100f
    );

    /** A map that closes in on the player. */
    private static final DimensionAtmosphere CLOSED = new MapAtmosphere(
            new Color(0, 40, 20),
            new Color(0, 80, 40),
            new Color(0, 60, 30),
            new Color(0, 4, 2),
            0.0f, 0f, 40f, 20f
    );

    @Test
    @DisplayName("A share of zero leaves the open end untouched")
    void zeroKeepsTheOpenEnd() {
        DimensionAtmosphere blended = BlendedAtmosphere.between(OPEN, CLOSED, 0f);

        assertEquals(20f, blended.fogStartDistance());
        assertEquals(200f, blended.fogEndDistance());
        assertEquals(1.0f, blended.skyLightFactor());
        assertEquals(OPEN.fogColor(), blended.fogColor());
    }

    @Test
    @DisplayName("A share of one is the map itself")
    void oneIsTheMapItself() {
        DimensionAtmosphere blended = BlendedAtmosphere.between(OPEN, CLOSED, 1f);

        assertEquals(0f, blended.fogStartDistance());
        assertEquals(40f, blended.fogEndDistance());
        assertEquals(0.0f, blended.skyLightFactor());
        assertEquals(CLOSED.fogColor(), blended.fogColor());
    }

    @Test
    @DisplayName("A share in between lands in between, on every value")
    void aShareLandsInBetween() {
        DimensionAtmosphere blended = BlendedAtmosphere.between(OPEN, CLOSED, 0.5f);

        assertEquals(10f, blended.fogStartDistance());
        assertEquals(120f, blended.fogEndDistance());
        assertEquals(60f, blended.skyFogEndDistance());
        assertEquals(0.5f, blended.skyLightFactor());
    }

    @Test
    @DisplayName("Colours are mixed channel by channel")
    void coloursAreMixedPerChannel() {
        DimensionAtmosphere blended = BlendedAtmosphere.between(OPEN, CLOSED, 0.5f);

        assertEquals(new Color(50, 70, 60), blended.fogColor());
        assertEquals(new Color(100, 140, 120), blended.skyLightColor());
        assertEquals(new Color(75, 105, 90), blended.skyColor());
    }

    @Test
    @DisplayName("A share outside its range is pulled back into it")
    void aShareOutsideItsRangeIsClamped() {
        assertEquals(OPEN.fogEndDistance(), BlendedAtmosphere.between(OPEN, CLOSED, -1f).fogEndDistance());
        assertEquals(CLOSED.fogEndDistance(), BlendedAtmosphere.between(OPEN, CLOSED, 2f).fogEndDistance());
    }

    @Test
    @DisplayName("The fog keeps a span the client can fade over")
    void theFogKeepsAFadeableSpan() {
        DimensionAtmosphere flat = new MapAtmosphere(
                new Color(0, 0, 0), new Color(0, 0, 0), new Color(0, 0, 0), new Color(1, 1, 1),
                0f, 30f, 31f, 20f
        );

        DimensionAtmosphere blended = BlendedAtmosphere.between(flat, flat, 0.5f);

        assertEquals(30f, blended.fogStartDistance());
        assertEquals(31f, blended.fogEndDistance(),
                "a blend must not collapse the gap the fog fades over");
    }

    @Test
    @DisplayName("Blending an atmosphere with itself changes nothing")
    void blendingWithItselfChangesNothing() {
        DimensionAtmosphere blended = BlendedAtmosphere.between(CLOSED, CLOSED, 0.3f);

        assertEquals(CLOSED.fogColor(), blended.fogColor());
        assertEquals(CLOSED.fogEndDistance(), blended.fogEndDistance());
        assertEquals(CLOSED.skyLightFactor(), blended.skyLightFactor());
    }
}
