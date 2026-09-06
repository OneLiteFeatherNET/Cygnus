package net.onelitefeather.cygnus.phase;

import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.bootstrap.ServiceShutdown;

import java.time.temporal.ChronoUnit;

/**
 * The {@link RestartPhase} is the last phase in the game cycle loop.
 * It will kick all players after a certain time and stops the server.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 **/
public final class RestartPhase extends TimedPhase {

    private static final Component KICK_MESSAGE = Component.text("The game is over. Thanks for playing it. <3", NamedTextColor.RED);

    /**
     * Creates a new instance from the {@link RestartPhase}.
     */
    public RestartPhase() {
        super("Restart", ChronoUnit.SECONDS, 1);
        this.setCurrentTicks(15);
        this.setEndTicks(-1);
    }


    /**
     * Ends the process once the countdown has run out.
     * <p>
     * This runs on the tick thread, so the actual work is handed to {@link ServiceShutdown}, which
     * stops the server from a thread of its own and then exits the JVM. Stopping without exiting
     * would leave the process alive - LuckPerms keeps a non-daemon thread that only its JVM
     * shutdown hook releases - and CloudNet would keep reporting the service as running until it
     * kills it after a timeout.
     * </p>
     */
    @Override
    protected void onFinish() {
        ServiceShutdown.request();
    }

    /**
     * Handles the update process for the phase.
     */
    @Override
    public void onUpdate() {
        switch (getCurrentTicks()) {
            case 10, 3, 2, 1 -> sendRestartMessage();
            case 0 -> {
                for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                    onlinePlayer.kick(KICK_MESSAGE);
                }
            }
            default -> {
                // Nothing to do here
            }
        }
    }

    /**
     * Sends the restart message to all players.
     */
    private void sendRestartMessage() {
        Component message = Messages.withPrefix(Component.text("Restart in", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(getCurrentTicks(), NamedTextColor.GREEN))
                .append(Component.space())
                .append(getSecondComponent());
        Broadcaster.broadcast(message);
    }

    /**
     * Returns the component which contains the second text for the restart message.
     *
     * @return the component with the second text
     */
    private Component getSecondComponent() {
        String secondPart = getCurrentTicks() > 1 ? "seconds" : "second";
        return Component.text(secondPart, NamedTextColor.GRAY);
    }
}
