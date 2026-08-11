package net.onelitefeather.cygnus.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.blood.BloodDirection;
import net.onelitefeather.cygnus.blood.BloodSplatterService;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Throws a blood splatter on demand, so the drawings can be judged without waiting to be hit.
 * <p>
 * {@code /blood} picks a side at random, {@code /blood front|right|back|left} asks for one.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class BloodCommand extends Command {

    /**
     * Creates the command.
     *
     * @param service the service that throws the splatter
     */
    public BloodCommand(BloodSplatterService service) {
        super("blood");

        var direction = ArgumentType.Enum("side", BloodDirection.class)
                .setFormat(ArgumentEnum.Format.LOWER_CASED);

        this.setDefaultExecutor((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            BloodDirection[] sides = BloodDirection.values();
            service.splatter(player, sides[ThreadLocalRandom.current().nextInt(sides.length)]);
        });

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            service.splatter(player, context.get(direction));
        }, direction);
    }

    /**
     * Narrows a sender down to a player, since a splatter needs a screen to land on.
     *
     * @param sender the sender to narrow
     * @return the player, or {@code null} if the sender has no screen
     */
    private static @Nullable Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Messages.withMiniPrefix("<red>Only players can bleed."));
        return null;
    }
}
