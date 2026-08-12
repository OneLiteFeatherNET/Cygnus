package net.onelitefeather.cygnus.command;

import net.kyori.adventure.key.Key;
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
import net.onelitefeather.cygnus.overlay.ScreenOverlay;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the command used to throw a splatter without waiting to be hit.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class BloodCommandTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The command throws a splatter from the requested side")
    void splatterIsThrownFromTheRequestedSide(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        register(overlay);

        MinecraftServer.getCommandManager().execute(player, "blood left");

        assertNotNull(overlay.blood(), "the command has to put blood on screen");
    }

    @Test
    @DisplayName("Without a side the command picks one itself")
    void splatterWorksWithoutASide(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        register(overlay);

        MinecraftServer.getCommandManager().execute(player, "blood");

        assertNotNull(overlay.blood(), "the bare command still has to show something");
    }

    @Test
    @DisplayName("Every side of the splatter can be requested")
    void everySideCanBeRequested(Env env) {
        RecordingOverlay overlay = new RecordingOverlay();
        Player player = spawn(env);
        register(overlay);

        for (BloodDirection direction : BloodDirection.values()) {
            overlay.forget();
            MinecraftServer.getCommandManager().execute(player, "blood " + direction.name().toLowerCase());
            assertNotNull(overlay.blood(), "no splatter for " + direction);
        }
    }

    /**
     * Registers the command under test against the given overlay. The environment is shared across
     * the tests in this class, so any command left over from an earlier one — still drawing into
     * that test's overlay — has to go first.
     *
     * @param overlay the overlay the service draws into
     */
    private void register(RecordingOverlay overlay) {
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

    /**
     * Records what the service contributes, standing in for the title-backed overlay.
     */
    private static final class RecordingOverlay implements ScreenOverlay {

        private final Map<OverlayLayer, Key> layers = new EnumMap<>(OverlayLayer.class);

        @Override
        public void set(Player player, OverlayLayer layer, @Nullable Key texture) {
            if (texture == null) {
                this.layers.remove(layer);
                return;
            }
            this.layers.put(layer, texture);
        }

        @Override
        public void clear(Player player) {
            this.layers.clear();
        }

        /**
         * @return the texture currently on the blood layer, or {@code null} if there is none
         */
        private @Nullable Key blood() {
            return this.layers.get(OverlayLayer.BLOOD);
        }

        /**
         * Drops everything recorded so far, to tell repeated draws apart.
         */
        private void forget() {
            this.layers.clear();
        }
    }
}
