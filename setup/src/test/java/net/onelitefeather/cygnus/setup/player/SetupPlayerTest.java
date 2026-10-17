package net.onelitefeather.cygnus.setup.player;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MicrotusExtension.class)
class SetupPlayerTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(new SetupPlayerProvider());
    }

    // MicrotusExtension never cleans up an Env resolved at class level, so stop its server process here
    @AfterAll
    static void stopProcess(Env env) {
        env.process().stop();
    }

    @Test
    void testPlayerInstance(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        SetupPlayer setupPlayer = assertInstanceOf(SetupPlayer.class, player);
        assertNull(setupPlayer.getSurvivorToDelete());
        assertNull(setupPlayer.getPageToDelete());

        env.destroyInstance(instance, true);
    }
}
