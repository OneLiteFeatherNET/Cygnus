package net.onelitefeather.cygnus.creek.consequence;

import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Operation;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocatorRevealIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The survivor shows on the slender's locator bar at once, and only on his")
    void slenderTracksTheSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        Collector<TrackedWaypointPacket> slenderPackets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);
        Collector<TrackedWaypointPacket> survivorPackets = survivorConnection.trackIncoming(TrackedWaypointPacket.class);
        LocatorReveal reveal = new LocatorReveal(6, TickSections.NONE);

        reveal.reveal(survivor, slender);

        assertEquals(List.of(Operation.TRACK), operationsOf(slenderPackets.collect(), survivor));
        assertTrue(survivorPackets.collect().isEmpty());
        reveal.cleanUp();
    }

    @Test
    @DisplayName("The waypoint follows the survivor")
    void waypointFollowsTheSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        LocatorReveal reveal = new LocatorReveal(6, TickSections.NONE);
        reveal.reveal(survivor, slender);
        Collector<TrackedWaypointPacket> packets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);

        survivor.teleport(new Pos(20, 40, 30)).join();
        for (int tick = 0; tick < LocatorReveal.RESEND_TICKS + 1; tick++) {
            env.tick();
        }

        List<TrackedWaypointPacket> sent = packets.collect();
        assertFalse(sent.isEmpty());
        TrackedWaypointPacket last = sent.getLast();
        assertEquals(Operation.UPDATE, last.operation());
        assertEquals(new TrackedWaypointPacket.Target.Vec3i(new Pos(20, 40, 30)), last.waypoint().target());
        reveal.cleanUp();
    }

    @Test
    @DisplayName("Cleaning up takes the survivor off the locator bar")
    void cleanUpUntracks(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        LocatorReveal reveal = new LocatorReveal(6, TickSections.NONE);
        reveal.reveal(survivor, slender);
        Collector<TrackedWaypointPacket> packets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);

        reveal.cleanUp();

        assertEquals(List.of(Operation.UNTRACK), operationsOf(packets.collect(), survivor));
    }

    @Test
    @DisplayName("A second reveal of the same survivor extends the first instead of ending with it")
    void secondRevealExtends(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        Collector<TrackedWaypointPacket> packets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);
        LocatorReveal reveal = new LocatorReveal(1, TickSections.NONE);

        reveal.reveal(survivor, slender);
        for (int tick = 0; tick < 10; tick++) {
            env.tick();
        }
        reveal.reveal(survivor, slender);
        for (int tick = 0; tick < 15; tick++) {
            env.tick();
        }

        List<Operation> operations = operationsOf(packets.collect(), survivor);
        assertEquals(1, operations.stream().filter(Operation.TRACK::equals).count());
        assertFalse(operations.contains(Operation.UNTRACK), "the first reveal's end must not take the second one down");
        reveal.cleanUp();
    }

    @Test
    @DisplayName("Revealing another survivor takes the one shown before off the locator bar")
    void onlyTheNewestRevealShows(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(10, 40, 0));
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(5, 40, 0));
        LocatorReveal reveal = new LocatorReveal(6, TickSections.NONE);
        reveal.reveal(first, slender);
        Collector<TrackedWaypointPacket> packets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);

        reveal.reveal(second, slender);

        List<TrackedWaypointPacket> sent = packets.collect();
        assertEquals(List.of(Operation.UNTRACK), operationsOf(sent, first));
        assertEquals(List.of(Operation.TRACK), operationsOf(sent, second));
        reveal.cleanUp();
    }

    @Test
    @DisplayName("The survivor is warned")
    void survivorIsWarned(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Collector<SystemChatPacket> messages = survivorConnection.trackIncoming(SystemChatPacket.class);
        LocatorReveal reveal = new LocatorReveal(6, TickSections.NONE);

        reveal.reveal(survivor, slender);

        assertFalse(messages.collect().isEmpty());
        reveal.cleanUp();
    }

    private static List<Operation> operationsOf(List<TrackedWaypointPacket> packets, Player survivor) {
        return packets.stream()
                .filter(packet -> packet.waypoint().id().unify(survivor.getUuid()::equals, name -> false))
                .map(TrackedWaypointPacket::operation)
                .toList();
    }
}
