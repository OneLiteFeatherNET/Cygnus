package net.onelitefeather.cygnus.listener.game;

import net.theevilreaper.aves.util.Broadcaster;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;

public final class GameFinishListener implements Consumer<GameFinishEvent> {

    @Override
    public void accept(GameFinishEvent event) {
        var reason = event.reason();
        var player = event.player();

        Component endComponent = switch (reason) {
            case ALL_SURVIVOR_DEAD, SURVIVOR_LEFT -> Messages.getSlenderWinMessage(player);
            case TIME_OVER, ALL_PAGES_FOUND, SLENDER_LEFT -> Messages.SURVIVOR_WIN_MESSAGE;
        };
        Broadcaster.broadcast(endComponent);
        sendRoundSummary();
    }

    /**
     * Sends a per-player summary of this round's stats to each online {@link CygnusPlayer}.
     */
    private void sendRoundSummary() {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!(onlinePlayer instanceof CygnusPlayer cygnusPlayer)) continue;
            Component box = TeamHelper.isSlenderTeam(onlinePlayer)
                    ? Messages.getSlenderRoundSummaryComponent(onlinePlayer, cygnusPlayer.getKills())
                    : Messages.getSurvivorRoundSummaryComponent(onlinePlayer, cygnusPlayer.getPageFounds(), cygnusPlayer.hasDied());
            onlinePlayer.sendMessage(box);
        }
    }
}
