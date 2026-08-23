package net.onelitefeather.cygnus.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import net.onelitefeather.cygnus.gaze.SlenderGazeService;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.RecordingScreenOverlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the command used to preview the slender's glitch without him being there.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
class GlitchCommandTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A requested level is drawn right away")
    void levelIsDrawnOnRequest(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);

        MinecraftServer.getCommandManager().execute(player, "glitch 2");

        assertNotNull(overlay.of(player, OverlayLayer.GLITCH), "the command has to put the glitch on screen");
    }

    @Test
    @DisplayName("Switching the preview off clears the screen")
    void offClearsTheScreen(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);
        MinecraftServer.getCommandManager().execute(player, "glitch 2");

        MinecraftServer.getCommandManager().execute(player, "glitch off");

        assertNull(overlay.of(player, OverlayLayer.GLITCH), "the preview must disappear");
    }

    @Test
    @DisplayName("Every level of the tearing can be requested")
    void everyLevelCanBeRequested(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);

        for (int level = 1; level <= SlenderGaze.LEVELS; level++) {
            overlay.clear(player);
            MinecraftServer.getCommandManager().execute(player, "glitch " + level);
            assertNotNull(overlay.of(player, OverlayLayer.GLITCH), "no glitch for level " + level);
        }
    }

    /**
     * Registers the command under test against the given overlay. The environment is shared across
     * the tests in this class, so any command left over from an earlier one — still drawing into
     * that test's overlay — has to go first.
     *
     * @param overlay the overlay the service draws into
     */
    private void register(RecordingScreenOverlay overlay) {
        Command previous = MinecraftServer.getCommandManager().getCommand("glitch");
        if (previous != null) MinecraftServer.getCommandManager().unregister(previous);
        MinecraftServer.getCommandManager().register(new GlitchCommand(new SlenderGazeService(overlay, () -> null)));
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
}
