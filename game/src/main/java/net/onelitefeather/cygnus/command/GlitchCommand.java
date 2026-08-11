package net.onelitefeather.cygnus.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import net.onelitefeather.cygnus.gaze.SlenderGazeService;
import org.jetbrains.annotations.Nullable;

/**
 * Puts the slender's glitch on screen without him being there, so the drawings can be judged from
 * the lobby.
 * <p>
 * {@code /glitch <1-4>} holds one level, {@code /glitch off} takes it away.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class GlitchCommand extends Command {

    /**
     * Creates the command.
     *
     * @param service the service that draws the tearing
     */
    public GlitchCommand(SlenderGazeService service) {
        super("glitch");

        var level = ArgumentType.Integer("level").between(1, SlenderGaze.LEVELS);

        this.setDefaultExecutor((sender, context) -> sender.sendMessage(
                Messages.withMiniPrefix("<gray>Usage: <yellow>/glitch <1-" + SlenderGaze.LEVELS + "> | off")
        ));

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            service.show(player, context.get(level) - 1);
        }, level);

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            service.hide(player);
        }, ArgumentType.Literal("off"));
    }

    /**
     * Narrows a sender down to a player, since the tearing needs a screen to sit on.
     *
     * @param sender the sender to narrow
     * @return the player, or {@code null} if the sender has none
     */
    private static @Nullable Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Messages.withMiniPrefix("<red>Only players have a view to lose."));
        return null;
    }
}
