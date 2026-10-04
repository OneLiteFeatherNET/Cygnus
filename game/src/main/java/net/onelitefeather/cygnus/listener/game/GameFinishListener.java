package net.onelitefeather.cygnus.listener.game;

import net.theevilreaper.aves.util.Broadcaster;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;

public final class GameFinishListener implements Consumer<GameFinishEvent> {

    /** What the winning side hears at the end of the round. */
    static final Key WIN_SOUND = Key.key("ui.toast.challenge_complete");

    /** What the losing side hears at the end of the round. */
    static final Key LOSS_SOUND = Key.key("entity.elder_guardian.curse");

    @Override
    public void accept(GameFinishEvent event) {
        var reason = event.reason();
        var player = event.player();

        boolean slenderWon = switch (reason) {
            case ALL_SURVIVOR_DEAD, SURVIVOR_LEFT -> true;
            case TIME_OVER, ALL_PAGES_FOUND, SLENDER_LEFT -> false;
        };
        Component endComponent = slenderWon ? Messages.getSlenderWinMessage(player) : Messages.SURVIVOR_WIN_MESSAGE;
        Broadcaster.broadcast(endComponent);
        sendRoundSummary(slenderWon);
    }

    /**
     * Plays every online player the sound of their side's win or loss and sends each online
     * {@link CygnusPlayer} a summary of this round's stats. Everyone who is not the slender is on
     * the survivors' side, the dead and the spectators included.
     *
     * @param slenderWon whether the slender won the round
     */
    private void sendRoundSummary(boolean slenderWon) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            boolean won = TeamHelper.isSlenderTeam(onlinePlayer) == slenderWon;
            onlinePlayer.playSound(Sound.sound(won ? WIN_SOUND : LOSS_SOUND, Sound.Source.MASTER, 1.0F, 1.0F),
                    Sound.Emitter.self());
            if (!(onlinePlayer instanceof CygnusPlayer cygnusPlayer)) continue;
            Component box = TeamHelper.isSlenderTeam(onlinePlayer)
                    ? Messages.getSlenderRoundSummaryComponent(onlinePlayer, cygnusPlayer.getKills())
                    : Messages.getSurvivorRoundSummaryComponent(onlinePlayer, cygnusPlayer.getPageFounds(), cygnusPlayer.hasDied());
            onlinePlayer.sendMessage(box);
        }
    }
}
