package net.onelitefeather.cygnus.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.component.Equippable;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.overlay.EquipmentScreenOverlay;
import net.onelitefeather.cygnus.tunnelvision.OverlayTunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the command used to eyeball the vignette while the round has not started yet.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
class TunnelVisionCommandTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A requested stage is drawn right away")
    void stageIsDrawnOnRequest(Env env) {
        Player player = spawn(env);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision stage 5");

        assertEquals(textureOf(5), cameraOverlay(player), "the command must draw the requested stage");
    }

    @Test
    @DisplayName("Switching the preview off clears the screen")
    void offClearsTheScreen(Env env) {
        Player player = spawn(env);
        register();
        MinecraftServer.getCommandManager().execute(player, "tunnelvision stage 5");

        MinecraftServer.getCommandManager().execute(player, "tunnelvision off");

        assertTrue(player.getHelmet().isAir(), "the preview must disappear");
    }

    @Test
    @DisplayName("A previewed intensity starts at its stage")
    void intensityStartsDrawing(Env env) {
        Player player = spawn(env);
        register();

        MinecraftServer.getCommandManager().execute(player, "tunnelvision intensity 1.0");

        assertEquals(
                textureOf(TunnelVisionStage.MAX_STAGE),
                cameraOverlay(player),
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
                new TunnelVisionCommand(new OverlayTunnelVisionRenderer(new EquipmentScreenOverlay())));
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
     * Reads the camera overlay the player is currently wearing.
     *
     * @param player the player to read
     * @return the overlay texture as a string
     */
    private String cameraOverlay(Player player) {
        Equippable equippable = player.getHelmet().get(DataComponents.EQUIPPABLE);
        assertNotNull(equippable, "nothing is carrying an overlay");
        return equippable.cameraOverlay();
    }

    /**
     * Builds the texture expected for a stage.
     *
     * @param stage the stage
     * @return the texture as a string
     */
    private String textureOf(int stage) {
        return "cygnus:gui/tunnel_vision/stage_" + stage;
    }
}
