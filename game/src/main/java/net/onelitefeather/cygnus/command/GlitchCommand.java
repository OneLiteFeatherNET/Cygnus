package net.onelitefeather.cygnus.command;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.gaze.BossBarGazeSignal;
import net.onelitefeather.cygnus.gaze.SlenderGaze;

/**
 * Puts one gaze level on the sender's screen and leaves it there, for judging the effect without a
 * slender to walk in front of.
 *
 * <p>This drives {@link BossBarGazeSignal} directly rather than going through
 * {@code SlenderGazeService}: the point of the preview is to judge a level on its own. During a
 * round the service overwrites the chosen level on its next pass, so the command is only useful
 * outside one - which is exactly where a player must not be able to reach it.</p>
 *
 * <p>Hence {@value #PERMISSION}. The gaze itself never runs in the lobby: the service starts on
 * {@code GameStartEvent} and clears every survivor on {@code GameFinishEvent}. This command was the
 * one way around that, and it is now closed to anyone without the node.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class GlitchCommand extends Command {

    private static final String PERMISSION = "cygnus.command.glitch";

    /**
     * Creates the command.
     *
     * @param signal the sink the level is written to
     */
    public GlitchCommand(BossBarGazeSignal signal) {
        super("glitch");
        setCondition((sender, commandString) -> !(sender instanceof Player) || hasGlitchPermission(sender));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Messages.getGlitchUsageMessage(SlenderGaze.LEVELS)));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            signal.detach(player);
            sender.sendMessage(Messages.withPrefix(
                    Component.text("Glitch signal off.", NamedTextColor.GRAY)));
        }, ArgumentType.Literal("off"));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            int level = context.get("level");
            if (level < 1 || level > SlenderGaze.LEVELS) {
                sender.sendMessage(Messages.getGlitchUsageMessage(SlenderGaze.LEVELS));
                return;
            }

            if (signal.barOf(player) == null) {
                signal.attach(player);
            }
            // The command speaks in the levels a player sees, 1 to 4. SlenderGaze counts from 0.
            signal.level(player, level - 1);
            sender.sendMessage(Messages.withPrefix(
                    Component.text("Glitch signal at level " + level + ".", NamedTextColor.GRAY)));
        }, ArgumentType.Integer("level"));
    }

    /**
     * Checks whether the given sender is allowed to run this command.
     *
     * <p>Reads Adventure's {@link PermissionChecker#POINTER}, which our player implementation backs
     * with LuckPerms (see {@code PermissionAwarePlayer}), the same way {@code StopCommand} does. A
     * sender without that pointer is denied.</p>
     *
     * @param sender the sender to check
     * @return {@code true} if the sender holds {@value #PERMISSION}, {@code false} otherwise
     */
    private static boolean hasGlitchPermission(CommandSender sender) {
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE))
                .test(PERMISSION);
    }
}
