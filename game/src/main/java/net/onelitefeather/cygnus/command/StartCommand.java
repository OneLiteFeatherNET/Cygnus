package net.onelitefeather.cygnus.command;

import net.theevilreaper.xerus.api.phase.LinearPhaseSeries;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.phase.LobbyPhase;

/**
 * This command allows players to force start the game. Non-player senders (the console) may always use it,
 * players need {@value #PERMISSION}.
 *
 * @author theEvilReaper
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.0.0
 **/
public class StartCommand extends Command {

    private static final String PERMISSION = "cygnus.command.start";

    public StartCommand(LinearPhaseSeries<TimedPhase> timedPhase) {
        super("start");
        setCondition((sender, commandString) -> !(sender instanceof Player) || hasStartPermission(sender));
        Component unableToStart = Messages.withPrefix(
                Component.text("Unable to start the game because the timer is too low!", NamedTextColor.RED)
        );
        addSyntax((sender, context) -> {
            if (!(timedPhase.getCurrentPhase() instanceof LobbyPhase lobbyPhase)) return;
            if (lobbyPhase.isPaused()) {
                sender.sendMessage(Messages.PHASE_NOT_RUNNING);
                return;
            }

            if (lobbyPhase.getCurrentTicks() < GameConfig.FORCE_START_TIME + 1) {
                sender.sendMessage(unableToStart);
                return;
            }

            if (lobbyPhase.isForceStarted()) {
                sender.sendMessage(Messages.ALREADY_FORCE_STARTED);
                return;
            }

            lobbyPhase.setForceStarted(true);
            sender.sendMessage(Messages.PHASE_FORCE_STARTED);
        });
    }

    /**
     * Checks whether the given sender is allowed to run this command.
     * <p>
     * Reads Adventure's {@link PermissionChecker#POINTER}, which our player implementation backs
     * with LuckPerms (see {@code PermissionAwarePlayer}). A sender without that pointer is denied.
     *
     * @param sender the sender to check
     * @return {@code true} if the sender holds {@value #PERMISSION}, {@code false} otherwise
     * @since 2.18.4
     */
    private static boolean hasStartPermission(CommandSender sender) {
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE))
                .test(PERMISSION);
    }
}
