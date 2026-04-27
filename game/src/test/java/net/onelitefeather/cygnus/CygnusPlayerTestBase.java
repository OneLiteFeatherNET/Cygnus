package net.onelitefeather.cygnus;

import net.minestom.server.entity.Player;
import net.minestom.server.network.PlayerProvider;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The class is a small utility class which overwrites the given {@link PlayerProvider} from the test environment to use the {@link CygnusPlayer} instead of the default {@link Player}.
 * This allows us to test the stamina system and other player related features without having to create a new player provider for each test class.
 * @author Joltra
 * @version 1.0.0
 * @since 2.3.0
 */
@ExtendWith(MicrotusExtension.class)
public abstract class CygnusPlayerTestBase {

    @BeforeAll
    static void init(Env env) {
        env.process().connection().setPlayerProvider(CygnusPlayer::new);
    }
}
