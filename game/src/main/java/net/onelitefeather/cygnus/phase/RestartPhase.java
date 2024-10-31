package net.onelitefeather.cygnus.phase;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

/**
 * The {@link RestartPhase} is the last phase in the game cycle loop.
 * It will kick all players after a certain time and stops the server.
 *
 * @author theEvilReaper
 * @version 1.0.0
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


    @Override
    protected void onFinish() {
        MinecraftServer.stopCleanly();
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
    private @NotNull Component getSecondComponent() {
        String secondPart = getCurrentTicks() > 1 ? "seconds" : "second";
        return Component.text(secondPart, NamedTextColor.GRAY);
    }
}
