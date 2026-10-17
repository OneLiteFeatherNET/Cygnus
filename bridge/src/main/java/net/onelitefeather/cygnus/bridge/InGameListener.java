package net.onelitefeather.cygnus.bridge;

import net.onelitefeather.cygnus.common.event.RoundInGameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Reacts to {@link RoundInGameEvent} by switching the service into its in game state, once.
 * <p>
 * Failures are logged and never propagate into the game thread.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class InGameListener implements Consumer<RoundInGameEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InGameListener.class);

    private final ServiceStateSwitch stateSwitch;
    private final AtomicBoolean switched = new AtomicBoolean();

    /**
     * Creates a new listener.
     *
     * @param stateSwitch the switch to trigger
     */
    public InGameListener(ServiceStateSwitch stateSwitch) {
        this.stateSwitch = stateSwitch;
    }

    @Override
    public void accept(RoundInGameEvent event) {
        if (!switched.compareAndSet(false, true)) return;
        try {
            stateSwitch.switchToInGame();
        } catch (Throwable throwable) {
            LOGGER.error("Could not switch the service to its in game state", throwable);
        }
    }
}
