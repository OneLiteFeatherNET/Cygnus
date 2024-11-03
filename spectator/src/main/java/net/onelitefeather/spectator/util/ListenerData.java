package net.onelitefeather.spectator.util;

import org.jetbrains.annotations.NotNull;

/**
 * The {@link ListenerData} is a data class to store listener options for the spectator module.
 *
 * @param detektSpectatorQuit if the quit event should be detected
 * @param detektSpectatorJoin if the join event should be detected
 * @param detektSpectatorChat if the chat event should be detected
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record ListenerData(boolean detektSpectatorQuit, boolean detektSpectatorJoin, boolean detektSpectatorChat) {

    /**
     * Creates a new instance of the {@link ListenerData} with the given parameters.
     *
     * @param detektSpectatorQuit if the quit event should be detected
     * @param detektSpectatorChat if the chat event should be detected
     * @return a new instance of the {@link ListenerData}
     */
    public static @NotNull ListenerData of(boolean detektSpectatorQuit, boolean detektSpectatorChat) {
        return new ListenerData(detektSpectatorQuit, false, detektSpectatorChat);
    }
}
