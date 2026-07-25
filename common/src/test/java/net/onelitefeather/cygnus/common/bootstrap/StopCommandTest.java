package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.command.CommandSender;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class StopCommandTest {

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
}
