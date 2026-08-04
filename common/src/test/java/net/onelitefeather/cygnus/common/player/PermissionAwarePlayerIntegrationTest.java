package net.onelitefeather.cygnus.common.player;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that a player answers permission questions without LuckPerms present, which is the state
 * every test run is in.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
@ExtendWith(MicrotusExtension.class)
class PermissionAwarePlayerIntegrationTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(TestPlayer::new);
    }

    @Test
    void testPermissionIsGrantedWithoutLuckPerms(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionAwarePlayer permissionAware = assertInstanceOf(PermissionAwarePlayer.class, player);
        assertEquals(TriState.TRUE, permissionAware.value("cygnus.test"));

        env.destroyInstance(instance, true);
    }

    @Test
    void testRepeatedFallbackChecksDoNotThrow(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionAwarePlayer permissionAware = assertInstanceOf(PermissionAwarePlayer.class, player);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                assertEquals(TriState.TRUE, permissionAware.value("cygnus.test"));
                assertEquals(TriState.TRUE, permissionAware.value("cygnus.other"));
            }
        });

        env.destroyInstance(instance, true);
    }

    @Test
    void testPointerResolvesThroughTheSamePath(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionChecker checker = player.getOrDefault(
                PermissionChecker.POINTER,
                PermissionChecker.always(TriState.FALSE)
        );
        assertEquals(TriState.TRUE, checker.value("cygnus.test"));

        env.destroyInstance(instance, true);
    }

    /**
     * A player which adds nothing to {@link PermissionAwarePlayer}, so the test observes the
     * permission handling of the base class and nothing else.
     */
    private static final class TestPlayer extends PermissionAwarePlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
