package net.onelitefeather.cygnus.phase;

import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.view.GameViewImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePhaseTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("The clean up runs although the series replaces the finished callback")
    void runsCleanUp(@NotNull Env env) {
        AtomicBoolean cleanedUp = new AtomicBoolean();
        GamePhase phase = new GamePhase(new GameViewImpl(), () -> cleanedUp.set(true), 10, new JumpScareManager());
        // LinearPhaseSeries#startCurrentPhase does exactly this before it starts a phase
        phase.setFinishedCallback(() -> {});

        phase.finish();

        assertTrue(cleanedUp.get());
    }
}
