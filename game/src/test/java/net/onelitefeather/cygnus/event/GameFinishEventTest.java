package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(value = {MicrotusExtension.class})
class GameFinishEventTest {

    private static Instance testInstance;
    private static Player testPlayer;

    @BeforeAll
    static void init(@NotNull Env env) {
        testInstance = env.createFlatInstance();
        testPlayer = env.createPlayer(testInstance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(testInstance, true);
        testInstance = null;
        testPlayer = null;
    }

    @ParameterizedTest
    @EnumSource(GameFinishEvent.Reason.class)
    void testFinishCreationWithEachReason(@NotNull GameFinishEvent.Reason reason) {
        GameFinishEvent event = new GameFinishEvent(reason, testPlayer);
        assertNotNull(event);
        assertEquals(reason, event.reason());
        assertEquals(testPlayer, event.player());
    }
}