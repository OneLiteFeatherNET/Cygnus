package net.onelitefeather.cygnus.command;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.RecordingScreenOverlay;
import net.onelitefeather.cygnus.tunnelvision.OverlayTunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the command used to eyeball the vignette while the round has not started yet.
 * <p>
 * The command is registered once for the whole class, so the overlay it draws into has to outlive a
 * single test as well. How a layer reaches the head slot is {@code EquipmentScreenOverlayTest}'s
 * business; what is checked here is which stage the command asks for.
 * </p>
 *
 * @author TheMeinerLP
 * @version 2.1.0
 * @since 2.7.0
 */
class TunnelVisionCommandTest extends CygnusPlayerTestBase {

    private static final RecordingScreenOverlay OVERLAY = new RecordingScreenOverlay();

    @Test
    @DisplayName("A requested stage is drawn right away")
    void stageIsDrawnOnRequest(Env env) {
        Player player = spawn(env);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision stage 5");

        assertEquals(textureOf(5), drawnFor(player), "the command must draw the requested stage");
    }

    @Test
    @DisplayName("Switching the preview off clears the screen")
    void offClearsTheScreen(Env env) {
        Player player = spawn(env);
        register();
        MinecraftServer.getCommandManager().execute(player, "tunnelvision stage 5");

        MinecraftServer.getCommandManager().execute(player, "tunnelvision off");

        assertTrue(OVERLAY.isEmpty(player), "the preview must disappear");
    }

    @Test
    @DisplayName("A previewed intensity starts at its stage")
    void intensityStartsDrawing(Env env) {
        Player player = spawn(env);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision intensity 1.0");

        assertEquals(
                textureOf(TunnelVisionStage.MAX_STAGE),
                drawnFor(player),
                "full intensity starts at the tightest stage"
        );
    }

    /**
     * Registers the command under test. The environment is shared across the tests in this class,
     * so a second registration would be rejected.
     */
    private void register() {
        if (MinecraftServer.getCommandManager().getCommand("tunnelvision") != null) return;
        MinecraftServer.getCommandManager().register(
                new TunnelVisionCommand(new OverlayTunnelVisionRenderer(OVERLAY)));
    }

    /**
     * Connects a player into a fresh instance.
     *
     * @param env the test environment
     * @return the connected player
     */
    private Player spawn(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    /**
     * Reads the tunnel vision texture the command last drew for a player.
     *
     * @param player the player to read
     * @return the texture currently on the tunnel vision layer, or {@code null} if there is none
     */
    private Key drawnFor(Player player) {
        return OVERLAY.of(player, OverlayLayer.TUNNEL_VISION);
    }

    /**
     * Builds the texture expected for a stage.
     *
     * @param stage the stage
     * @return the texture key
     */
    private Key textureOf(int stage) {
        return Key.key("cygnus", "gui/tunnel_vision/stage_" + stage);
    }
}
