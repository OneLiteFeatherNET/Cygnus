package net.onelitefeather.cygnus.pass;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers who may change slender pass balances: the console always, a player only with the permission.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
@ExtendWith(MicrotusExtension.class)
class PassCommandTest {

    @Test
    void consoleMayChangeBalances(@NotNull Env env) {
        assertTrue(PassCommand.mayAdminister(MinecraftServer.getCommandManager().getConsoleSender()),
                "the console has the admin rights implicitly");
    }

    @Test
    void playerWithoutThePermissionMayNotChangeBalances(@NotNull Env env) {
        Player player = env.createPlayer(env.createFlatInstance());

        assertFalse(PassCommand.mayAdminister(player), "a player needs cygnus.pass.admin");
    }
}
