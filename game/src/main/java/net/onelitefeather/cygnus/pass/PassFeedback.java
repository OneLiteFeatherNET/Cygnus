package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Delivers what the slender pass service says to the players: messages and the balance on the lobby item. The
 * service only names players by UUID, so it can be tested without a server.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public interface PassFeedback {

    /**
     * Sends a message to one player, if the player is online.
     *
     * @param player  the player
     * @param message the message
     */
    void tell(UUID player, Component message);

    /**
     * Sends a message to every online player except one.
     *
     * @param message the message
     * @param except  the player who does not get it
     */
    void tellLobby(Component message, UUID except);

    /**
     * Shows a balance on the player's lobby item.
     *
     * @param player  the player
     * @param balance the balance
     */
    void showBalance(UUID player, int balance);
}
