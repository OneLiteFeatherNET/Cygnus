package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The event is called when the game reaches a finish condition.
 * All conditions are related to some events or conditions
 * @param reason the reason why the game reaches his final state
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 *
 */
public record GameFinishEvent(@NotNull Reason reason, @Nullable Player player) implements Event {

    public GameFinishEvent(@NotNull Reason reason) {
        this(reason, null);
    }

    /**
     * The enum class contains all available reason why a came could end.
     * @author theEvilReaper
     * @version 1.0.0
     * @since 1.0.0
     */
    public enum Reason {

        TIME_OVER,
        ALL_PAGES_FOUND,
        ALL_SURVIVOR_DEAD,
        SLENDER_LEFT,
        SURVIVOR_LEFT;
    }
}
