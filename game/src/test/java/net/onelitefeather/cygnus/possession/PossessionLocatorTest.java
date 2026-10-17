package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Operation;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionLocatorTest extends CygnusPlayerTestBase {

    private static List<Operation> operationsOf(List<TrackedWaypointPacket> packets, Player survivor) {
        return packets.stream()
                .filter(packet -> packet.waypoint().id().unify(survivor.getUuid()::equals, name -> false))
                .map(TrackedWaypointPacket::operation)
                .toList();
    }

    @Test
    @DisplayName("Survivors show on the locator bar while they are in range and leave it once they leave the range")
    void followsTheRange(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0, 40, 0));
        Player first = env.createPlayer(instance, new Pos(5, 40, 0));
        Player second = env.createPlayer(instance, new Pos(10, 40, 0));
        PossessionLocator locator = new PossessionLocator();
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        locator.update(slender, Set.of(first, second));
        locator.update(slender, Set.of(first));

        assertEquals(Set.of(first), locator.tracked());
        List<TrackedWaypointPacket> sent = packets.collect();
        assertEquals(List.of(Operation.TRACK, Operation.UNTRACK), operationsOf(sent, second));
        assertEquals(List.of(Operation.TRACK, Operation.UPDATE), operationsOf(sent, first), "a tracked survivor only gets his new position");
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Clearing takes every survivor off the locator bar")
    void clearUntracksAll(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0, 40, 0));
        Player survivor = env.createPlayer(instance, new Pos(5, 40, 0));
        PossessionLocator locator = new PossessionLocator();
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        locator.update(slender, Set.of(survivor));
        locator.clear(slender);

        assertTrue(locator.tracked().isEmpty());
        assertEquals(List.of(Operation.TRACK, Operation.UNTRACK), operationsOf(packets.collect(), survivor));
        env.destroyInstance(instance, true);
    }
}
