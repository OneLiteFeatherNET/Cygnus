package net.onelitefeather.cygnus.command;

import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.player.PermissionAwarePlayer;
import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the permission condition of the {@link StartCommand}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.4
 */
@ExtendWith(MicrotusExtension.class)
class StartCommandTest {

    private static final String PERMISSION = "cygnus.command.start";

    @BeforeAll
    static void setUp(Env env) {
        env.process().connection().setPlayerProvider(GrantingPlayer::new);
    }

    // MicrotusExtension never cleans up an Env resolved at class level, so stop its server process here
    @AfterAll
    static void stopProcess(Env env) {
        env.process().stop();
    }

    private static StartCommand newCommand() {
        return new StartCommand(new LinearPhaseSeries<TimedPhase>("TestSeries"));
    }

    @Test
    @DisplayName("A player without the permission cannot use the command")
    void playerWithoutPermissionIsDenied(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        assertFalse(newCommand().getCondition().canUse(player, "start"),
                "A player without " + PERMISSION + " must not see or run /start");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A player holding the permission can use the command")
    void playerWithPermissionIsAllowed(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ((GrantingPlayer) player).grant(PERMISSION);

        assertTrue(newCommand().getCondition().canUse(player, "start"),
                "A player holding " + PERMISSION + " must be able to run /start");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A player holding only an unrelated permission is denied")
    void playerWithOtherPermissionIsDenied(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ((GrantingPlayer) player).grant("cygnus.command.stop");

        assertFalse(newCommand().getCondition().canUse(player, "start"),
                "Only " + PERMISSION + " may unlock /start");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The console is always allowed to use the command")
    void consoleIsAllowed(@NotNull Env env) {
        assertTrue(newCommand().getCondition().canUse(env.process().command().getConsoleSender(), "start"),
                "Non-player senders must stay allowed");
    }

    /**
     * A player whose permissions are exactly the nodes granted to this instance.
     */
    private static final class GrantingPlayer extends PermissionAwarePlayer {

        private final Set<String> granted = new HashSet<>();

        private GrantingPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }

        private void grant(String permission) {
            this.granted.add(permission);
        }

        @Override
        public @NotNull TriState value(@NotNull String permission) {
            return TriState.byBoolean(this.granted.contains(permission));
        }
    }
}
