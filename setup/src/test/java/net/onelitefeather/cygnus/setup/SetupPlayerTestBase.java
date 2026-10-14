package net.onelitefeather.cygnus.setup;

import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
public abstract class SetupPlayerTestBase {

    @BeforeAll
    static void setup(Env env) {
        env.process().connection().setPlayerProvider(new SetupPlayerProvider());
    }
}
