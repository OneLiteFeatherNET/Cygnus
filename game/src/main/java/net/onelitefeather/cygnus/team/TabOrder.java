package net.onelitefeather.cygnus.team;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Orders the tab list by the role a player has in the round.
 * <p>
 * Since 1.21.2 the client sorts the tab list by the list order of an entry first (a higher number is listed
 * higher, see {@link Player#setListOrder(int)}) and only then falls back to the team name and the player
 * name, compared case-insensitively. Giving every role its own list order therefore yields the slender
 * first, the living survivors next and the dead last, while the players of one role stay sorted by name
 * without the server doing any work for it.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TabOrder {

    /** The slender is listed on top. */
    public static final int SLENDER = 2;

    /** The living survivors are listed below the slender. */
    public static final int SURVIVOR = 1;

    /** Spectators and everybody outside a round are listed last. */
    public static final int SPECTATOR = 0;

    /**
     * Returns the list order of the role with the given team key.
     *
     * @param teamKey the value of the {@link Tags#TEAM_KEY} tag, may be {@code null}
     * @return the list order, {@link #SPECTATOR} for any unknown role
     */
    public static int of(@Nullable Key teamKey) {
        if (GameConfig.SLENDER_KEY.equals(teamKey)) return SLENDER;
        if (GameConfig.SURVIVOR_KEY.equals(teamKey)) return SURVIVOR;
        return SPECTATOR;
    }

    /**
     * Sets the list order of the player from the team they currently carry.
     *
     * @param player the player to order
     */
    public static void apply(Player player) {
        player.setListOrder(of(player.getTag(Tags.TEAM_KEY)));
    }

    /**
     * Puts the player back to the default position they have outside of a round.
     *
     * @param player the player to reset
     */
    public static void reset(Player player) {
        player.setListOrder(SPECTATOR);
    }

    private TabOrder() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
