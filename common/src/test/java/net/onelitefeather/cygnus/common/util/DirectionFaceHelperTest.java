package net.onelitefeather.cygnus.common.util;

import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DirectionFaceHelperTest {

    @ParameterizedTest(name = "Test direction parsing for {0}")
    @ValueSource(strings = { "NORTH", "SOUTH", "EAST", "WEST" })
    void testValidDirectionParsing(String face) {
        Direction parsedDirection = DirectionFaceHelper.parseDirection(face);
        assertNotNull(parsedDirection);
        assertNotEquals(Direction.UP, parsedDirection);
        assertNotEquals(Direction.DOWN, parsedDirection);
    }

    @Test
    void testInvalidDirectionParsing() {
        assertEquals(Direction.NORTH, DirectionFaceHelper.parseDirection("INVALID"));
        assertEquals(Direction.NORTH, DirectionFaceHelper.parseDirection(""));
    }

    @ParameterizedTest
    @ValueSource(doubles = { -51, 51 })
    void testValidFaceByPitch(double pitch) {
        assertFalse(DirectionFaceHelper.isValidFace(pitch));
    }

    @ParameterizedTest
    @ValueSource(doubles = { -50, 0, 50 })
    void testValidFace(double pitch) {
        assertTrue(DirectionFaceHelper.isValidFace(pitch));
    }

    @ParameterizedTest
    @ValueSource(doubles = { 51, 100, 200 })
    void testInvalidFaceGetByPitch(double pitch) {
        Direction invalidDirection = DirectionFaceHelper.getInvalidDirection(pitch);
        assertNotNull(invalidDirection);
        assertNotHorizontal(invalidDirection);
    }

    /**
     * Asserts that the given direction is not horizontal.
     * @param direction the direction to check
     */
    private void assertNotHorizontal(@NotNull Direction direction) {
        for (int i = 0; i < Direction.HORIZONTAL.length; i++) {
            assertNotEquals(Direction.HORIZONTAL[i], direction);
        }
    }
}
