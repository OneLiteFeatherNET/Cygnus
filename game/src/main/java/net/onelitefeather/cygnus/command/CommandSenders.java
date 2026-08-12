package net.onelitefeather.cygnus.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.Nullable;

/**
 * Narrows a {@link CommandSender} down to a {@link Player}, since every preview command in this
 * package draws on a screen and only a player has one.
 * <p>
 * {@code TunnelVisionCommand}, {@code BloodCommand} and {@code GlitchCommand} each hand-rolled an
 * identical {@code private static @Nullable Player asPlayer(CommandSender)}, differing only in the
 * error string sent back to the console. This type is that method, extracted once.
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
     * @param sender the sender to narrow
     * @param reason the rest of the sentence after {@code "Only players "}, e.g. {@code "can bleed."}
     * @return the player, or {@code null} if the sender has no screen to draw on
     */
    public static @Nullable Player asPlayer(CommandSender sender, String reason) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Messages.withMiniPrefix("<red>Only players " + reason));
        return null;
    }
}
