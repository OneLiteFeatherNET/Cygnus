package net.onelitefeather.cygnus.ambient;

import net.minestom.server.entity.Player;

import java.util.List;

/**
 * Hears about the team-wide lights-out of {@link AmbientProvider}.
 * <p>
 * A seam for tracing: the provider calls it once per blackout, after the effects are applied and the
 * next interval is rolled, and does nothing else with it.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface BlackoutObserver {

    /**
     * Hears nothing.
     */
    BlackoutObserver NONE = (_, _, _) -> {
    };

    /**
     * A blackout just happened.
     *
     * @param affected        the players it hit
     * @param durationTicks   how long the blindness lasts
     * @param nextInSeconds   the interval just rolled until the next blackout
     */
    void blackout(List<Player> affected, int durationTicks, int nextInSeconds);
}
