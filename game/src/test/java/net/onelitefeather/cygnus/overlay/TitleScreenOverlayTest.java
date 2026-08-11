package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetTitleTextPacket;
import net.minestom.server.network.packet.server.play.SetTitleTimePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the layers of the HUD overlay end up in one title without erasing each other.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TitleScreenOverlayTest extends CygnusPlayerTestBase {

    private static final Component TUNNEL = Component.text("T");
    private static final Component BLOOD = Component.text("B");

    private final TitleScreenOverlay overlay = new TitleScreenOverlay();

    @Test
    @DisplayName("A single layer reaches the screen on its own")
    void singleLayerIsSent(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        collector.assertSingle(packet -> assertEquals("T", plain(packet.title())));
    }

    @Test
    @DisplayName("Two layers are stacked with the blood on top")
    void layersAreStacked(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);

        collector.assertSingle(packet -> {
            String text = plain(packet.title());
            assertEquals(3, text.length(), "the layers need a spacer between them: " + text.length());
            assertEquals('T', text.charAt(0), "the tunnel vision is drawn first");
            assertEquals('B', text.charAt(2), "the blood is drawn on top");
        });
    }

    @Test
    @DisplayName("Dropping one layer leaves the other standing")
    void droppingOneLayerKeepsTheOther(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.overlay.set(player, OverlayLayer.BLOOD, null);

        collector.assertSingle(packet -> assertEquals("T", plain(packet.title()), "the tunnel vision must survive"));
    }

    @Test
    @DisplayName("Clearing empties the screen")
    void clearingEmptiesTheScreen(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.overlay.clear(player);

        collector.assertSingle(packet -> assertTrue(plain(packet.title()).isEmpty()));
    }

    @Test
    @DisplayName("A cleared player starts from an empty screen again")
    void clearingForgetsTheLayers(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        this.overlay.clear(player);
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);

        collector.assertSingle(packet -> assertEquals("B", plain(packet.title()), "the old layer must be gone"));
    }

    @Test
    @DisplayName("The title is set to hold, once per player")
    void timesAreSentOnce(Env env) {
        TestConnection connection = connect(env);
        Player player = player(connection, env);
        Collector<SetTitleTimePacket> first = connection.trackIncoming(SetTitleTimePacket.class);

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        first.assertSingle(packet -> {
            assertEquals(0, packet.fadeIn(), "a fade in would make the overlay pump on every update");
            assertEquals(0, packet.fadeOut(), "a fade out would do the same");
            assertTrue(packet.stay() > 0, "the overlay has to survive between updates");
        });

        Collector<SetTitleTimePacket> second = connection.trackIncoming(SetTitleTimePacket.class);
        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);
        second.assertEmpty();
    }

    /**
     * Opens a connection to the test environment.
     *
     * @param env the test environment
     * @return the connection
     */
    private TestConnection connect(Env env) {
        return env.createConnection();
    }

    /**
     * Connects a player through the given connection.
     *
     * @param connection the connection to use
     * @param env        the test environment
     * @return the connected player
     */
    private Player player(TestConnection connection, Env env) {
        Instance instance = env.createFlatInstance();
        return connection.connect(instance, new Pos(0, 40, 0));
    }

    /**
     * Serialises a component down to its bare text.
     *
     * @param component the component to serialise
     * @return the plain text
     */
    private String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
