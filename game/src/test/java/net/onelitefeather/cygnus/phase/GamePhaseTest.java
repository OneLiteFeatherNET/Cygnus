package net.onelitefeather.cygnus.phase;

import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePhaseTest extends CygnusPlayerTestBase {

    @Test
    void testEndRunnableSurvivesTheSeriesReplacingTheFinishedCallback(@NotNull Env env) {
        AtomicBoolean cleanedUp = new AtomicBoolean();
        GamePhase phase = new GamePhase(new GameViewImpl(), () -> cleanedUp.set(true), 10, new JumpScareManager());
        // LinearPhaseSeries#startCurrentPhase does exactly this before it starts a phase
        phase.setFinishedCallback(() -> {});

        phase.finish();

        assertTrue(cleanedUp.get(), "the round's clean up must run even though the series owns the finished callback");
    }
}
