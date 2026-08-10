package net.onelitefeather.cygnus.command;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.tunnelvision.ActionBarTunnelVisionRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the command used to eyeball the vignette while the round has not started yet.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TunnelVisionCommandTest extends CygnusPlayerTestBase {

    /** First glyph of the pack font; stage 1 lives here, the rest follows consecutively. */
    private static final int FIRST_CODE_POINT = 0xE000;

    @Test
    @DisplayName("A requested stage is drawn right away")
    void stageIsDrawnOnRequest(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision stage 5");

        collector.assertSingle(packet -> assertEquals(
                glyphOf(5),
                plain(packet),
                "the command must draw the requested stage"
        ));
    }

    @Test
    @DisplayName("Switching the preview off clears the screen")
    void offClearsTheScreen(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision off");

        collector.assertSingle(packet -> assertTrue(plain(packet).isEmpty(), "the preview must disappear"));
    }

    @Test
    @DisplayName("A previewed intensity starts at its stage")
    void intensityStartsDrawing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision intensity 1.0");

        collector.assertSingle(packet -> assertEquals(
                glyphOf(8),
                plain(packet),
                "full intensity starts at the tightest stage"
        ));
    }

    /**
     * Registers the command under test. The environment is shared across the tests in this class,
     * so a second registration would be rejected.
     */
    private void register() {
        if (MinecraftServer.getCommandManager().getCommand("tunnelvision") != null) return;
        MinecraftServer.getCommandManager().register(new TunnelVisionCommand(new ActionBarTunnelVisionRenderer()));
    }

    /**
     * Reads the bare text out of an action bar packet.
     *
     * @param packet the packet to read
     * @return the plain text
     */
    private String plain(ActionBarPacket packet) {
        return PlainTextComponentSerializer.plainText().serialize(packet.text());
    }

    /**
     * Builds the glyph expected for a stage.
     *
     * @param stage the stage
     * @return the glyph as a string
     */
    private String glyphOf(int stage) {
        return new String(Character.toChars(FIRST_CODE_POINT + stage - 1));
    }
}
