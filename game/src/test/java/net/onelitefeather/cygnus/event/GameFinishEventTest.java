package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class GameFinishEventTest {

    @Disabled("The test can't be executed due that Env is not compatible with a ParameterizedTest")
    @ParameterizedTest
    @EnumSource(GameFinishEvent.Reason.class)
    void testFinishCreationWithEachReason(@NotNull Env env, @NotNull GameFinishEvent.Reason reason) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        GameFinishEvent event = new GameFinishEvent(reason, player);

        assertNotNull(event);
        assertEquals(reason, event.reason());
        assertEquals(player, event.player());

        env.destroyInstance(instance, true);
    }
}