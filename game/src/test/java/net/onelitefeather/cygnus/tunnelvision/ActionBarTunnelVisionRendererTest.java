package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the vignette reaches the client as an action bar carrying the pack's font.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class ActionBarTunnelVisionRendererTest extends CygnusPlayerTestBase {

    private final ActionBarTunnelVisionRenderer renderer = new ActionBarTunnelVisionRenderer();

    @Test
    @DisplayName("A stage is sent as its glyph in the pack font")
    void stageIsSentAsGlyph(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        this.renderer.render(player, 3);

        collector.assertSingle(packet -> {
            Component message = packet.text();
            assertEquals(ActionBarTunnelVisionRenderer.FONT, message.style().font(), "the overlay must use the pack font");
            assertEquals(glyphOf(3), plain(message), "the glyph must match the stage");
        });
    }

    @Test
    @DisplayName("The overlay is drawn without a text shadow")
    void overlayHasNoShadow(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        this.renderer.render(player, 8);

        collector.assertSingle(packet -> assertEquals(
                ShadowColor.none(),
                packet.text().style().shadowColor(),
                "a shadow would render the vignette a second time, offset"
        ));
    }

    @Test
    @DisplayName("Clearing sends an empty action bar")
    void clearingSendsEmptyActionBar(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        this.renderer.clear(player);

        collector.assertSingle(packet -> assertTrue(
                plain(packet.text()).isEmpty(),
                "the overlay must disappear rather than linger"
        ));
    }

    @Test
    @DisplayName("Stage zero clears instead of drawing a glyph")
    void zeroStageClears(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        this.renderer.render(player, 0);

        collector.assertSingle(packet -> assertTrue(plain(packet.text()).isEmpty(), "stage zero has no glyph"));
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

    /**
     * Builds the glyph expected for a stage.
     *
     * @param stage the stage
     * @return the glyph as a string
     */
    private String glyphOf(int stage) {
        return new String(Character.toChars(ActionBarTunnelVisionRenderer.FIRST_CODE_POINT + stage - 1));
    }
}
