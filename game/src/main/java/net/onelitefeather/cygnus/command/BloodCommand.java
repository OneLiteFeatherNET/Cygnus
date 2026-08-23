package net.onelitefeather.cygnus.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.blood.BloodDirection;
import net.onelitefeather.cygnus.blood.BloodSplatterService;
import net.onelitefeather.cygnus.common.Messages;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Throws a blood splatter on demand, so the drawings can be judged without waiting to be hit.
 * <p>
 * {@code /blood} picks a side at random, {@code /blood front|right|back|left} asks for one.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
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
            Player player = CommandSenders.asPlayer(sender, Messages.ONLY_PLAYERS_CAN_BLEED);
            if (player == null) return;
            BloodDirection[] sides = BloodDirection.values();
            service.splatter(player, sides[ThreadLocalRandom.current().nextInt(sides.length)]);
        });

        this.addSyntax((sender, context) -> {
            Player player = CommandSenders.asPlayer(sender, Messages.ONLY_PLAYERS_CAN_BLEED);
            if (player == null) return;
            service.splatter(player, context.get(direction));
        }, direction);
    }
}
