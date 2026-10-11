package net.onelitefeather.cygnus.common.player;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.rank.RankTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that a player falls back to {@link RankTag#PLAYER} without LuckPerms present, which is
 * the state every test run is in.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MicrotusExtension.class)
class PermissionAwarePlayerRankTagTest {

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(TestPlayer::new);
    }

    @Test
    void testRankTagFallsBackToPlayerWithoutLuckPerms(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionAwarePlayer permissionAware = assertInstanceOf(PermissionAwarePlayer.class, player);
        assertEquals(RankTag.PLAYER, permissionAware.rankTag());

        env.destroyInstance(instance, true);
    }

    @Test
    void testApplyRankTagDisplayNameSetsThePrefixedName(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PermissionAwarePlayer permissionAware = assertInstanceOf(PermissionAwarePlayer.class, player);
        permissionAware.applyRankTagDisplayName();

        assertEquals(RankTag.PLAYER.prefix(Component.text(player.getUsername())), player.getDisplayName());

        env.destroyInstance(instance, true);
    }

    /**
     * A player which adds nothing to {@link PermissionAwarePlayer}, so the test observes the
     * rank tag handling of the base class and nothing else.
     */
    private static final class TestPlayer extends PermissionAwarePlayer {

        private TestPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}
