package net.onelitefeather.cygnus.tunnelvision;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
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
 * Verifies that the vignette reaches the client as a title carrying the pack's font.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class TitleTunnelVisionRendererTest extends CygnusPlayerTestBase {

    private final TitleTunnelVisionRenderer renderer = new TitleTunnelVisionRenderer();

    @Test
    @DisplayName("A stage is sent as its glyph in the pack font")
    void stageIsSentAsGlyph(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.renderer.render(player, 3);

        collector.assertSingle(packet -> {
            Component message = packet.title();
            assertEquals(TitleTunnelVisionRenderer.FONT, message.style().font(), "the overlay must use the pack font");
            assertEquals(glyphOf(3), plain(message), "the glyph must match the stage");
        });
    }

    @Test
    @DisplayName("The title is set to hold instead of fading")
    void titleHoldsWithoutFading(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetTitleTimePacket> collector = connection.trackIncoming(SetTitleTimePacket.class);

        this.renderer.render(player, 3);

        collector.assertSingle(packet -> {
            assertEquals(0, packet.fadeIn(), "a fade in would make the vignette pump on every update");
            assertEquals(0, packet.fadeOut(), "a fade out would do the same");
            assertTrue(packet.stay() > 0, "the vignette has to survive between updates");
        });
    }

    @Test
    @DisplayName("The times are sent once rather than with every update")
    void timesAreSentOnce(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        this.renderer.render(player, 3);
        Collector<SetTitleTimePacket> collector = connection.trackIncoming(SetTitleTimePacket.class);

        this.renderer.render(player, 4);
        this.renderer.render(player, 5);

        collector.assertEmpty();
    }

    @Test
    @DisplayName("The overlay is drawn without a text shadow")
    void overlayHasNoShadow(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.renderer.render(player, TunnelVisionStage.MAX_STAGE);

        collector.assertSingle(packet -> assertEquals(
                ShadowColor.none(),
                packet.title().style().shadowColor(),
                "a shadow would render the vignette a second time, offset"
        ));
    }

    @Test
    @DisplayName("Clearing empties the title")
    void clearingEmptiesTheTitle(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.renderer.clear(player);

        collector.assertSingle(packet -> assertTrue(
                plain(packet.title()).isEmpty(),
                "the overlay must disappear rather than linger"
        ));
    }

    @Test
    @DisplayName("Stage zero clears instead of drawing a glyph")
    void zeroStageClears(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetTitleTextPacket> collector = connection.trackIncoming(SetTitleTextPacket.class);

        this.renderer.render(player, 0);

        collector.assertSingle(packet -> assertTrue(plain(packet.title()).isEmpty(), "stage zero has no glyph"));
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
        return new String(Character.toChars(TitleTunnelVisionRenderer.FIRST_CODE_POINT + stage - 1));
    }
}
