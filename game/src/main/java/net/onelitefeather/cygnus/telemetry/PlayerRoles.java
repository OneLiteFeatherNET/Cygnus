package net.onelitefeather.cygnus.telemetry;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.team.TeamHelper;

/**
 * Names the role a player has in the round, for span attributes.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
final class PlayerRoles {

    private PlayerRoles() {
    }

    /**
     * Reads the role from the player's team tag. Valid only while the tag is set, which the death
     * listener removes: read it before that listener runs.
     *
     * @param player the player
     * @return {@code slender}, {@code survivor}, {@code spectator} or {@code none}
     */
    static String of(Player player) {
        if (TeamHelper.isSlenderTeam(player)) {
            return "slender";
        }
        if (TeamHelper.isSurvivorTeam(player)) {
            return "survivor";
        }
        return TeamHelper.isSpectatorTeam(player) ? "spectator" : "none";
    }
}
