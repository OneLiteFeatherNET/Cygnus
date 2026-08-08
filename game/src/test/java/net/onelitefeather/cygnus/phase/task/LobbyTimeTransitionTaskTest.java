package net.onelitefeather.cygnus.phase.task;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration and unit tests for {@link LobbyTimeTransitionTask} and its integration in {@link LobbyPhase}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
class LobbyTimeTransitionTaskTest extends CygnusPlayerTestBase {

    private void tickTicks(Env env, int count) {
        for (int i = 0; i < count; i++) {
            env.tick();
        }
    }

    @Test
    void testTimeTransitionProgress(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        instance.setTime(1000L); // Start at morning time

        LobbyTimeTransitionTask task = new LobbyTimeTransitionTask(() -> instance, Helper.MIDNIGHT_TIME, 1);

        assertFalse(task.isRunning(), "Task should not be running before start.");
        task.start();
        assertTrue(task.isRunning(), "Task should be running after start.");

        // Advance 10 ticks (halfway through 1 second / 20 ticks)
        tickTicks(env, 10);
        assertTrue(task.isRunning(), "Task should still be running after 10 ticks.");
        long timeMidway = instance.getTime();
        assertTrue(timeMidway > 1000L && timeMidway < Helper.MIDNIGHT_TIME, "Time should be interpolated midway.");

        // Advance remaining 10 ticks (total 20 ticks = 1 second)
        tickTicks(env, 10);
        assertFalse(task.isRunning(), "Task should complete after total ticks.");
        assertEquals(Helper.MIDNIGHT_TIME, instance.getTime(), "Time should reach target time at completion.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testResetRestoresInitialTime(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        instance.setTime(6000L); // Noon

        LobbyTimeTransitionTask task = new LobbyTimeTransitionTask(() -> instance, Helper.MIDNIGHT_TIME, 2);
        task.start();

        tickTicks(env, 10); // Advance halfway through
        assertTrue(task.isRunning());

        task.reset();
        assertFalse(task.isRunning(), "Task should be stopped after reset.");
        assertEquals(6000L, instance.getTime(), "Instance time should be restored to initial time after reset.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testLobbyPhaseTriggersTimeTransitionAtTenSeconds(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        instance.setTime(1000L);

        GameConfig config = GameConfig.builder()
                .lobbyTime(30)
                .minPlayers(1)
                .gameTime(600)
                .maxPlayers(10)
                .build();

        LobbyPhase lobbyPhase = new LobbyPhase(config, () -> instance);
        lobbyPhase.setCurrentTicks(10);
        lobbyPhase.onUpdate();

        // Tick environment to allow scheduled transition task to run
        tickTicks(env, 5);
        assertTrue(instance.getTime() > 1000L, "Time transition should have started after onUpdate at 10s.");

        env.destroyInstance(instance, true);
    }
}
