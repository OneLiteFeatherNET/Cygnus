package net.onelitefeather.cygnus.command;

import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.minestom.server.command.builder.Command;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.utils.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * The command allows to force start the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public class StartCommand extends Command {

    public StartCommand(@NotNull LinearPhaseSeries<TimedPhase> timedPhase) {
        super("start", "ss");

        addSyntax((sender, context) -> {
            if (!(timedPhase.getCurrentPhase() instanceof LobbyPhase lobbyPhase)) return;
            if (lobbyPhase.isPaused()) {
                sender.sendMessage(Messages.PHASE_NOT_RUNNING);
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
}
