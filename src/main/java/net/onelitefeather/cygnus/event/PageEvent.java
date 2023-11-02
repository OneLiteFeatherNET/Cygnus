package net.onelitefeather.cygnus.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.onelitefeather.cygnus.game.PageEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The event is called when a specific action was performed on a page entity. The action is defined by the inner
 * enum class {@link Reason}.
 * @param entity the page entity
 * @param reason the reason why the event was called
 * @param player the player who performed the action or null
 */
public record PageEvent(@NotNull PageEntity entity, @NotNull Reason reason, @Nullable Player player) implements Event {

    /**
     * Creates a new instance from the event without a player reference.
     * @param entity the page entity
     * @param reason the reason why the event was called
     */
    public PageEvent(@NotNull PageEntity entity, @NotNull Reason reason) {
        this(entity, reason, null);
    }

    /**
     * The inner enum class contains all reason why a page event could be called from the game.
     * <ul>
     *     <li>FOUND: The page was found by a player</li>
     *     <li>TTL: The page was not found in the given time</li>
     * </ul>
     */
    public enum Reason {

        FOUND, TTL
    }
}
