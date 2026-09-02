package net.onelitefeather.cygnus.common.util;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelperTest {

    @Test
    void testUpdatePositionNorth() {
        Pos origin = new Pos(10, 20, 30);
        Pos updated = Helper.updatePosition(origin, Direction.NORTH);
        assertEquals(10.5, updated.x());
        assertEquals(20.5, updated.y());
        assertEquals(31.0, updated.z());
        assertEquals(0.0f, updated.yaw());
        assertEquals(0.0f, updated.pitch());
    }

    @Test
    void testUpdatePositionSouth() {
        Pos origin = new Pos(10, 20, 30);
        Pos updated = Helper.updatePosition(origin, Direction.SOUTH);
        assertEquals(10.5, updated.x());
        assertEquals(20.5, updated.y());
        assertEquals(29.97, updated.z(), 0.001);
        assertEquals(0.0f, updated.yaw());
        assertEquals(0.0f, updated.pitch());
    }

    @Test
    void testUpdatePositionEast() {
        Pos origin = new Pos(10, 20, 30);
        Pos updated = Helper.updatePosition(origin, Direction.EAST);
        assertEquals(9.97, updated.x(), 0.001);
        assertEquals(20.5, updated.y());
        assertEquals(30.5, updated.z());
        assertEquals(-90.0f, updated.yaw());
        assertEquals(0.0f, updated.pitch());
    }

    @Test
    void testUpdatePositionWest() {
        Pos origin = new Pos(10, 20, 30);
        Pos updated = Helper.updatePosition(origin, Direction.WEST);
        assertEquals(11.0, updated.x());
        assertEquals(20.5, updated.y());
        assertEquals(30.5, updated.z());
        assertEquals(90.0f, updated.yaw(), "WEST facing page must have yaw 90");
        assertEquals(0.0f, updated.pitch());
    }

    @Test
    void testUpdatePositionUnsupportedDirection() {
        Pos origin = new Pos(10, 20, 30);
        assertThrows(IllegalArgumentException.class, () -> Helper.updatePosition(origin, Direction.UP));
        assertThrows(IllegalArgumentException.class, () -> Helper.updatePosition(origin, Direction.DOWN));
    }

    @Test
    void testCalculatePageTeleportPositionNorth() {
        Pos blockPos = new Pos(10, 20, 30);
        Pos teleportPos = Helper.calculatePageTeleportPosition(blockPos, Direction.NORTH);
        assertEquals(10.5, teleportPos.x());
        assertEquals(20.0, teleportPos.y());
        assertEquals(31.5, teleportPos.z());
        assertEquals(180.0f, teleportPos.yaw());
        assertEquals(0.0f, teleportPos.pitch());
    }

    @Test
    void testCalculatePageTeleportPositionSouth() {
        Pos blockPos = new Pos(10, 20, 30);
        Pos teleportPos = Helper.calculatePageTeleportPosition(blockPos, Direction.SOUTH);
        assertEquals(10.5, teleportPos.x());
        assertEquals(20.0, teleportPos.y());
        assertEquals(29.5, teleportPos.z());
        assertEquals(0.0f, teleportPos.yaw());
        assertEquals(0.0f, teleportPos.pitch());
    }

    @Test
    void testCalculatePageTeleportPositionEast() {
        Pos blockPos = new Pos(10, 20, 30);
        Pos teleportPos = Helper.calculatePageTeleportPosition(blockPos, Direction.EAST);
        assertEquals(9.5, teleportPos.x());
        assertEquals(20.0, teleportPos.y());
        assertEquals(30.5, teleportPos.z());
        assertEquals(-90.0f, teleportPos.yaw());
        assertEquals(0.0f, teleportPos.pitch());
    }

    @Test
    void testCalculatePageTeleportPositionWest() {
        Pos blockPos = new Pos(10, 20, 30);
        Pos teleportPos = Helper.calculatePageTeleportPosition(blockPos, Direction.WEST);
        assertEquals(11.5, teleportPos.x());
        assertEquals(20.0, teleportPos.y());
        assertEquals(30.5, teleportPos.z());
        assertEquals(90.0f, teleportPos.yaw());
        assertEquals(0.0f, teleportPos.pitch());
    }

    @Test
    void testCalculatePageTeleportPositionUnsupportedDirection() {
        Pos blockPos = new Pos(10, 20, 30);
        Pos teleportPos = Helper.calculatePageTeleportPosition(blockPos, Direction.UP);
        assertEquals(10.5, teleportPos.x());
        assertEquals(20.0, teleportPos.y());
        assertEquals(30.5, teleportPos.z());
    }
}
