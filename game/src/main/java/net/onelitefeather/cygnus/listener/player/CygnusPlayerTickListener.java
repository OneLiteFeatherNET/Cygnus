package net.onelitefeather.cygnus.listener.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Handles the per tick logic of a {@link Player} like the jump scare detection, the sprint blocking,
 * the heartbeat and the ambient sounds, which get more frequent the more scared a survivor is.
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 */
public final class CygnusPlayerTickListener implements Consumer<PlayerTickEvent> {

    private final JumpScareManager jumpscareManager;
    private final DreadSource dreadSource;
    private final LongSupplier clock;

    /**
     * Creates a new instance of this listener implementation
     *
     * @param jumpscareManager the jump scare manager instance
     * @param dreadSource      rates how scared a survivor is, to pace their ambient sounds
     * @param clock            supplies the current time in milliseconds
     */
    public CygnusPlayerTickListener(JumpScareManager jumpscareManager, DreadSource dreadSource, LongSupplier clock) {
        this.jumpscareManager = jumpscareManager;
        this.dreadSource = dreadSource;
        this.clock = clock;
    }

    @Override
    public void accept(PlayerTickEvent event) {
        Player player = event.getPlayer();

        boolean livingSurvivor = isLivingSurvivor(player);
        if (livingSurvivor) {
            this.jumpscareManager.checkTurnAround(player);
        }

        if (!(player instanceof CygnusPlayer cygnusPlayer)) return;

        if (cygnusPlayer.hasBlockedSprinting()) {
            cygnusPlayer.sendPacket(cygnusPlayer.getPropertiesPacket());
            cygnusPlayer.sendPacket(cygnusPlayer.getMetadataPacket());
        }

        cygnusPlayer.tickHeartbeat();

        if (livingSurvivor) {
            cygnusPlayer.tickAmbient(this.dreadSource.dreadOf(player.getUuid(), this.clock.getAsLong()));
        }
    }

    /**
     * Checks whether the given player is allowed to receive a jump scare and the ambient sounds.
     * <p>
     * A jump scare spawns a phantom corpse and applies {@code DARKNESS} for 40 ticks. Only survivors
     * may receive it: for the slender it would be a direct gameplay interference and for a spectator
     * it would blind a player that is not part of the round anymore. The check is fail closed, so an
     * untagged player never receives a scare either. The ambient sounds follow the survivor's fear, so
     * they are only for survivors, too.
     *
     * @param player the player to check
     * @return {@code true} if the player is a living survivor
     */
    private static boolean isLivingSurvivor(Player player) {
        return TeamHelper.isSurvivorTeam(player) && !player.isDead();
    }
}
