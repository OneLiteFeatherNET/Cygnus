package net.onelitefeather.spectator.util;

import org.jetbrains.annotations.NotNull;

/**
 * The {@link ListenerData} is a data class to store listener options for the spectator module.
 *
 * @param detectSpectatorQuit if the quit event should be detected
 * @param detectSpectatorJoin if the join event should be detected
 * @param detectSpectatorChat if the chat event should be detected
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record ListenerData(boolean detectSpectatorQuit, boolean detectSpectatorJoin, boolean detectSpectatorChat) {

    /**
     * Creates a new instance of the {@link ListenerData} with the given parameters.
     *
     * @param detectSpectatorQuit if the quit event should be detected
     * @param detectSpectatorChat if the chat event should be detected
     * @return a new instance of the {@link ListenerData}
     */
    public static @NotNull ListenerData of(boolean detectSpectatorQuit, boolean detectSpectatorChat) {
        return new ListenerData(detectSpectatorQuit, false, detectSpectatorChat);
    }
}
