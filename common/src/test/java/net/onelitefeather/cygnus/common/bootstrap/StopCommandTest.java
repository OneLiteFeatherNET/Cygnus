package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.player.PermissionAwarePlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class StopCommandTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(TestPlayer::new);
    }

    @Test
    void testCommandName() {
        StopCommand command = new StopCommand();
        assertEquals("stop", command.getName());
    }

    @Test
    void testConsoleSenderIsAlwaysAllowed(@NotNull Env env) {
        StopCommand command = new StopCommand();
        CommandSender consoleSender = env.process().command().getConsoleSender();

        assertTrue(command.getCondition().canUse(consoleSender, "stop"));
    }

    @Test
    void testPlayerIsAllowedWithoutLuckPerms(@NotNull Env env) {
        StopCommand command = new StopCommand();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertTrue(command.getCondition().canUse(player, "stop"));

        env.destroyInstance(instance, true);
    }

    /**
     * A player which adds nothing to {@link PermissionAwarePlayer}, so the command sees the same
     * pointer a Cygnus player carries.
     */
    private static final class TestPlayer extends PermissionAwarePlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
