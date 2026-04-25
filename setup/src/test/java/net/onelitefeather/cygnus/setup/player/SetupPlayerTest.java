package net.onelitefeather.cygnus.setup.player;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.setup.SetupPlayerTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetupPlayerTest extends SetupPlayerTestBase {

    @Test
    void testPlayerInstance(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertInstanceOf(SetupPlayer.class, player);

        SetupPlayer setupPlayer = (SetupPlayer) player;
        assertNull(setupPlayer.getSurvivorToDelete());
        assertNull(setupPlayer.getPageToDelete());

        env.destroyInstance(instance, true);
    }
}
