package net.onelitefeather.cygnus.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Narrows a {@link CommandSender} down to a {@link Player}, since every preview command in this
 * package draws on a screen and only a player has one.
 * <p>
 * On the branch these preview commands were cut from, {@code TunnelVisionCommand},
 * {@code BloodCommand} and {@code GlitchCommand} each hand-rolled an identical
 * {@code private static @Nullable Player asPlayer(CommandSender)}, differing only in the message
 * sent back to the console. This type is that method, extracted once, so that the three land on top
 * of it instead of bringing a fourth copy each. The message itself is passed in rather than
 * assembled here, so it can stay with the other player-facing texts in
 * {@link net.onelitefeather.cygnus.common.Messages}.
 * </p>
 * <p>
 * A static helper was chosen over an abstract base command on purpose. The narrowing check is the
 * only thing the three commands share — their constructors take different services, their default
 * executors print different usage lines, and {@code TunnelVisionCommand} alone runs a per-player
 * preview loop. An abstract base class would force every subclass into one constructor shape and
 * one inheritance chain to get a single one-line check, coupling command shape to something none of
 * them actually have in common. A stateless static method carries the shared behaviour without
 * dragging the unrelated parts of any one command onto the other two, which keeps each command free
 * to change its syntax, its executor and its scheduling independently.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class CommandSenders {

    private CommandSenders() {
    }

    /**
     * Narrows the given sender down to a player, telling them why not if it cannot.
     *
     * @param sender  the sender to narrow
     * @param message the message to send back when the sender is not a player, taken from
     *                {@link net.onelitefeather.cygnus.common.Messages} like every other
     *                player-facing text
     * @return the player, or {@code null} if the sender has no screen to draw on
     */
    public static @Nullable Player asPlayer(CommandSender sender, Component message) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(message);
        return null;
    }
}
