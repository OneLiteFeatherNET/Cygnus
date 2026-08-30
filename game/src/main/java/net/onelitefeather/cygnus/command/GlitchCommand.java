package net.onelitefeather.cygnus.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * {@code SlenderGazeService}: the point of the preview is to test the channel on its own. Were the
 * service running against the same sink, it would overwrite the chosen level on its next pass and
 * a failure would be impossible to attribute.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class GlitchCommand extends Command {

    /**
     * Creates the command.
     *
     * @param signal the sink the level is written to
     */
    public GlitchCommand(BossBarGazeSignal signal) {
        super("glitch");

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
}
