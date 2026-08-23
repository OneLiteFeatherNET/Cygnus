package net.onelitefeather.cygnus.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.blood.BloodDirection;
import net.onelitefeather.cygnus.blood.BloodSplatterService;
import net.onelitefeather.cygnus.overlay.OverlayLayer;
import net.onelitefeather.cygnus.overlay.RecordingScreenOverlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the command used to throw a splatter without waiting to be hit.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.7.0
 */
class BloodCommandTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The command throws a splatter from the requested side")
    void splatterIsThrownFromTheRequestedSide(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);

        MinecraftServer.getCommandManager().execute(player, "blood left");

        assertNotNull(overlay.of(player, OverlayLayer.BLOOD), "the command has to put blood on screen");
    }

    @Test
    @DisplayName("Without a side the command picks one itself")
    void splatterWorksWithoutASide(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);

        MinecraftServer.getCommandManager().execute(player, "blood");

        assertNotNull(overlay.of(player, OverlayLayer.BLOOD), "the bare command still has to show something");
    }

    @Test
    @DisplayName("Every side of the splatter can be requested")
    void everySideCanBeRequested(Env env) {
        RecordingScreenOverlay overlay = new RecordingScreenOverlay();
        Player player = spawn(env);
        register(overlay);

        for (BloodDirection direction : BloodDirection.values()) {
            overlay.clear(player);
            MinecraftServer.getCommandManager().execute(player, "blood " + direction.name().toLowerCase());
            assertNotNull(overlay.of(player, OverlayLayer.BLOOD), "no splatter for " + direction);
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
        Command previous = MinecraftServer.getCommandManager().getCommand("blood");
        if (previous != null) MinecraftServer.getCommandManager().unregister(previous);
        MinecraftServer.getCommandManager().register(new BloodCommand(new BloodSplatterService(overlay, bound -> 0)));
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
