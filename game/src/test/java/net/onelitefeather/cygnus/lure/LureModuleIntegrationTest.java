package net.onelitefeather.cygnus.lure;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.world.InstanceGround;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureModuleIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The lure starts with the round and stops when it ends")
    void startsOnGameStartAndStopsOnFinish(Env env) {
        Instance instance = env.createFlatInstance();
        LureService service = service(instance);
        LureModule module = new LureModule(LureConfig.DEFAULT, service);

        module.node().call(new GameStartEvent());
        assertTrue(service.isRunning());

        module.node().call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        assertFalse(service.isRunning());
    }

    @Test
    @DisplayName("The lure's own switch decides whether it takes part")
    void disabledModuleIsOff(Env env) {
        Instance instance = env.createFlatInstance();
        LureConfig off = new LureConfig(false, 30, 0.08D, 0.30D, 4, 60);

        assertFalse(new LureModule(off, service(instance)).enabled());
        assertTrue(new LureModule(LureConfig.DEFAULT, service(instance)).enabled());
    }

    private static LureService service(Instance instance) {
        return new LureService(LureConfig.DEFAULT, List::of, new JumpScareManager(TickSections.NONE),
                new LureSpotFinder(new InstanceGround(() -> instance), () -> instance),
                new LureAftermath(LureConfig.DEFAULT, (_, _) -> {}, new CreekRespite(), () -> null, (_, _) -> {},
                        () -> LureOutcome.NOTHING),
                () -> 0L, new Random(1), TickSections.NONE);
    }
}
