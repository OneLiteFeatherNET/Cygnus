package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.stats.SlenderPassRepository;

import java.util.function.Consumer;

/**
 * The {@code /pass} command. Players reserve, cancel and check their own passes; {@value #PERMISSION} holders and
 * the console change balances with {@code give}, {@code take} and {@code set}.
 * <p>
 * While passes are not available every subcommand answers that the feature is unavailable.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PassCommand extends Command {

    /** The permission that lets a player change balances. The console has it implicitly. */
    static final String PERMISSION = "cygnus.pass.admin";

    private static final String SLENDER = "slender";
    private static final String CANCEL = "cancel";
    private static final String BALANCE = "balance";
    private static final String GIVE = "give";
    private static final String TAKE = "take";
    private static final String SET = "set";

    private final SlenderPassService service;

    /**
     * Creates the command and its syntaxes.
     *
     * @param service the service the subcommands act on
     */
    public PassCommand(SlenderPassService service) {
        super("pass");
        this.service = service;

        Argument<String> player = ArgumentType.Word("player");
        Argument<Integer> amount = ArgumentType.Integer("amount");

        addSyntax((sender, context) -> playerAction(sender, this.service::reserve));
        addSyntax((sender, context) -> playerAction(sender, this.service::reserve), ArgumentType.Literal(SLENDER));
        addSyntax((sender, context) -> playerAction(sender, this.service::cancel), ArgumentType.Literal(CANCEL));
        addSyntax((sender, context) -> playerAction(sender, this.service::balance), ArgumentType.Literal(BALANCE));
        addSyntax((sender, context) -> admin(sender, context.get(player), context.get(amount), Operation.GIVE),
                ArgumentType.Literal(GIVE), player, amount);
        addSyntax((sender, context) -> admin(sender, context.get(player), context.get(amount), Operation.TAKE),
                ArgumentType.Literal(TAKE), player, amount);
        addSyntax((sender, context) -> admin(sender, context.get(player), context.get(amount), Operation.SET),
                ArgumentType.Literal(SET), player, amount);
    }

    /**
     * Tells whether the sender may change balances. Players need {@value #PERMISSION} through the permission
     * pointer of the server; the console always may.
     *
     * @param sender the sender to check
     * @return {@code true} for the console and for players who hold the permission
     */
    static boolean mayAdminister(CommandSender sender) {
        return !(sender instanceof Player)
                || sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE)).test(PERMISSION);
    }

    private void playerAction(CommandSender sender, Consumer<Player> action) {
        if (!this.service.available()) {
            sender.sendMessage(Messages.PASS_UNAVAILABLE);
            return;
        }
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            sender.sendMessage(Messages.PASS_ONLY_PLAYERS);
        }
    }

    private void admin(CommandSender sender, String name, int amount, Operation operation) {
        if (!this.service.available()) {
            sender.sendMessage(Messages.PASS_UNAVAILABLE);
            return;
        }
        if (!mayAdminister(sender)) {
            sender.sendMessage(Messages.PASS_NO_PERMISSION);
            return;
        }
        if (amount < 0) {
            sender.sendMessage(Messages.PASS_NEGATIVE_AMOUNT);
            return;
        }
        Player target = MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (target == null) {
            sender.sendMessage(Messages.PASS_TARGET_OFFLINE);
            return;
        }
        Consumer<SlenderPassRepository.Adjustment> reply = result -> {
            if (operation == Operation.TAKE && result.delta() == 0) {
                sender.sendMessage(Messages.PASS_NOTHING_REMOVED);
            } else {
                sender.sendMessage(Messages.passChanged(target.getUsername(), result.balance()));
            }
        };
        switch (operation) {
            case GIVE -> this.service.give(target.getUuid(), amount, reply);
            case TAKE -> this.service.take(target.getUuid(), amount, reply);
            case SET -> this.service.set(target.getUuid(), amount, reply);
        }
    }

    private enum Operation {
        GIVE, TAKE, SET
    }
}
