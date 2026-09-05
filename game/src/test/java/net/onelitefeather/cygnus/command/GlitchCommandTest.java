package net.onelitefeather.cygnus.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.gaze.BossBarGazeSignal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies who may reach the glitch preview.
 *
 * <p>The gaze itself never runs outside a round - the service starts on {@code GameStartEvent} and
 * clears everyone on {@code GameFinishEvent}. This command was the one way to put the effect on a
 * screen in the lobby, which is why it is gated.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.9.3
 */
class GlitchCommandTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The command is named glitch")
    void testCommandName() {
        assertEquals("glitch", new GlitchCommand(new BossBarGazeSignal()).getName());
    }

    @Test
    @DisplayName("The command carries a condition at all")
    void testConditionIsSet() {
        assertNotNull(new GlitchCommand(new BossBarGazeSignal()).getCondition(),
                "without a condition any player could put the effect on their screen in the lobby");
    }

    @Test
    @DisplayName("The console may always run it")
    void testConsoleSenderIsAlwaysAllowed(@NotNull Env env) {
        GlitchCommand command = new GlitchCommand(new BossBarGazeSignal());
        CommandSender console = env.process().command().getConsoleSender();

        assertTrue(command.getCondition().canUse(console, "glitch"));
    }

    @Test
    @DisplayName("A player is allowed while LuckPerms is absent, as everywhere else")
    void testPlayerIsAllowedWithoutLuckPerms(@NotNull Env env) {
        GlitchCommand command = new GlitchCommand(new BossBarGazeSignal());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        // Without LuckPerms on the class path every check answers TRUE, so tests reach gated paths
        // at all. On a real server the node decides.
        assertTrue(command.getCondition().canUse(player, "glitch"));

        env.destroyInstance(instance, true);
    }
}
