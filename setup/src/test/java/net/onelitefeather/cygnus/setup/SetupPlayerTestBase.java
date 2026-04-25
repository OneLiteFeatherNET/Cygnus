package net.onelitefeather.cygnus.setup;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(MicrotusExtension.class)
public abstract class SetupPlayerTestBase {

    @BeforeAll
    static void setup(Env env) {
        env.process().connection().setPlayerProvider(new SetupPlayerProvider());
    }

    @Test
    void testPlayerInstance(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertInstanceOf(SetupPlayer.class, player);

        env.destroyInstance(instance, true);
    }
}
