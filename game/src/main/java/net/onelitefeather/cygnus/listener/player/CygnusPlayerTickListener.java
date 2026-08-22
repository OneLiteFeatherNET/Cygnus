package net.onelitefeather.cygnus.listener.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;

/**
 * Handles the per tick logic of a {@link Player} like the jump scare detection, the sprint blocking
 * and the heartbeat.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class CygnusPlayerTickListener implements Consumer<PlayerTickEvent> {

    private final JumpScareManager jumpscareManager;

    public CygnusPlayerTickListener(JumpScareManager jumpscareManager) {
        this.jumpscareManager = jumpscareManager;
    }

    @Override
    public void accept(PlayerTickEvent event) {
        Player player = event.getPlayer();

        if (isJumpScareTarget(player)) {
            this.jumpscareManager.checkTurnAround(player);
        }

        if (!(player instanceof CygnusPlayer cygnusPlayer)) return;

        if (cygnusPlayer.hasBlockedSprinting()) {
            cygnusPlayer.sendPacket(cygnusPlayer.getPropertiesPacket());
            cygnusPlayer.sendPacket(cygnusPlayer.getMetadataPacket());
        }

        cygnusPlayer.tickHeartbeat();
    }

    /**
     * Checks whether the given player is allowed to receive a jump scare.
     * <p>
     * A jump scare spawns a phantom corpse and applies {@code DARKNESS} for 40 ticks. Only survivors
     * may receive it: for the slender it would be a direct gameplay interference and for a spectator
     * it would blind a player that is not part of the round anymore. The check is fail closed, so an
     * untagged player never receives a scare either.
     *
     * @param player the player to check
     * @return {@code true} if the player is a living survivor
     */
    private static boolean isJumpScareTarget(Player player) {
        return TeamHelper.isSurvivorTeam(player) && !player.isDead();
    }
}
