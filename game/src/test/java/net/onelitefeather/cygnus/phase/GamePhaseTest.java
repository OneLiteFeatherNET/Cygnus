package net.onelitefeather.cygnus.phase;

import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.Phase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePhaseTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The clean up callback runs and the series still advances")
    void runsCleanUp(@NotNull Env env) {
        AtomicBoolean cleanedUp = new AtomicBoolean();
        GamePhase phase = new GamePhase(new GameViewImpl(), 10, new JumpScareManager(), _ -> 0.0D);
        phase.addFinishedCallback(() -> cleanedUp.set(true));
        Phase nextPhase = new Phase("NextPhase") {
            @Override
            protected void onStart() {
                // Nothing to do, the test only checks that the series reaches this phase
            }
        };
        LinearPhaseSeries<Phase> series = new LinearPhaseSeries<>("TestSeries");
        series.add(phase);
        series.add(nextPhase);
        series.start();

        phase.finish();

        assertTrue(cleanedUp.get());
        assertSame(nextPhase, series.getCurrentPhase());
    }
}
