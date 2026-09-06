package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Drains the stamina of a survivor who starts sprinting and blocks the sprint while the bar is empty.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class PlayerStartSprintingListener implements Consumer<PlayerStartSprintingEvent> {

    private final Function<Player, FoodBar> staminaFunction;

    public PlayerStartSprintingListener(Function<Player, FoodBar> staminaFunction) {
        this.staminaFunction = staminaFunction;
    }

    /**
     * Consumes stamina for the sprinting player, unless they are not running a round.
     * <p>
     * The check is limited to the survivors and is fail closed, mirroring
     * {@link PlayerStopSprintingListener}. Only excluding the slender let a spectator - who keeps flying
     * and sprinting after their death - drain the stamina bar they owned as a survivor, which showed up
     * as a draining experience bar and a sprint block. Once the bar is handed back on death the lookup
     * answers {@code null} for them as well, so the guard also keeps this off a missing bar.
     *
     * @param event the sprint event to handle
     */
    @Override
    public void accept(PlayerStartSprintingEvent event) {
        var player = event.getPlayer();
        if (!player.hasTag(Tags.TEAM_KEY)) return;
        if (!TeamHelper.isSurvivorTeam(player)) return;

        CygnusPlayer cygnusPlayer = (CygnusPlayer) player;

        if (cygnusPlayer.hasBlockedSprinting()) {
            event.setCancelled(true);
            return;
        }

        FoodBar staminaBarRef = staminaFunction.apply(player);
        if (!staminaBarRef.canConsume()) {
            event.setCancelled(true);
            return;
        }
        staminaBarRef.startConsume();
    }
}
