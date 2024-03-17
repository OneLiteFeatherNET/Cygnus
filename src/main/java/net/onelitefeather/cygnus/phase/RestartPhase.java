package net.onelitefeather.cygnus.phase;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.utils.Messages;

import java.time.temporal.ChronoUnit;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.cygnus.utils.Helper.MINI_MESSAGE;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/

public final class RestartPhase extends TimedPhase {

    private static final Component KICK_MESSAGE = MINI_MESSAGE.deserialize("<red>The game is over. Thanks for playing it. <3");

    public RestartPhase() {
        super("Restart", ChronoUnit.SECONDS, 1);
        this.setCurrentTicks(15);
        this.setEndTicks(-1);
    }

    @Override
    protected void onFinish() {
        MinecraftServer.stopCleanly();
    }

    @Override
    public void onUpdate() {
        switch (getCurrentTicks()) {
            case 10, 3, 2, 1 -> Broadcaster.broadcast(Messages.withMiniPrefix("<green>Restart in " + getCurrentTicks() + secondPart()));
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

    private @NotNull String secondPart() {
        return getCurrentTicks() > 1 ? " seconds" : "second";
    }
}
