package net.onelitefeather.cygnus.visibility;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Predicate;

/**
 * Single source of truth for the role visibility matrix of the game.
 * <p>
 * Who (row) may see whom (column) as an entity:
 * <table border="1">
 *     <caption>Role visibility matrix</caption>
 *     <tr><th></th><th>Slender</th><th>Survivor</th><th>Spectator</th></tr>
 *     <tr><th>Slender</th><td>-</td><td>yes</td><td>never</td></tr>
 *     <tr><th>Survivor</th><td>only while revealed</td><td>yes</td><td>never</td></tr>
 *     <tr><th>Spectator</th><td>same as the survivor view</td><td>yes</td><td>yes</td></tr>
 * </table>
 * <p>
 * The matrix is expressed exclusively as per-viewer predicates handed to
 * {@link Entity#updateViewableRule(Predicate)}. This is deliberately the <i>only</i> visibility mechanism in
 * the code base: the manual packet level ({@code Entity#updateNewViewer(Player)} /
 * {@code Entity#updateOldViewer(Player)}) does not touch the internal viewer bit set and therefore
 * desynchronizes with the rule, which produced both duplicated and missing spawn packets before.
 * <p>
 * Two properties of the Minestom implementation (verified against {@code 2026.07.22-26.2},
 * {@code EntityView.java}) shape this class:
 * <ul>
 *     <li>{@link Entity#updateViewableRule(Predicate)} installs a rule, {@link Entity#updateViewableRule()}
 *     re-evaluates the already installed one. Passing {@code null} is not a reset: it only ever adds
 *     viewers and never removes any.</li>
 *     <li>{@code Entity#addViewer(Player)} / {@code Entity#removeViewer(Player)} must never be used here.
 *     They record the player in {@code EntityView#manualViewers}, and {@code EntityView.Option#update}
 *     permanently skips those players, which would disable the rule for them for good.</li>
 * </ul>
 * Survivors intentionally get <b>no</b> rule at all: the Minestom default (auto viewable) already means
 * "visible to everybody", which is exactly the survivor row of the matrix.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
public final class VisibilityRules {

    /**
     * Builds the viewable rule for the slender player.
     * <p>
     * The slender is visible to everybody while revealed and to nobody while hidden. The rule ignores the
     * viewer on purpose: spectators share the survivor view of the slender, so there is no viewer group
     * that is allowed to peek through {@link Tags#HIDDEN}.
     *
     * @param slender the player acting as the slender
     * @return the per-viewer predicate to install on the slender
     */
    public static Predicate<Player> slenderRule(Player slender) {
        return _ -> !isHidden(slender);
    }

    /**
     * Builds the viewable rule for a spectator.
     * <p>
     * Spectators see each other but stay invisible for every player that is still in the round.
     *
     * @return the per-viewer predicate to install on a spectator
     */
    public static Predicate<Player> spectatorRule() {
        return TeamHelper::isSpectatorTeam;
    }

    /**
     * Checks whether the given player is currently hidden according to {@link Tags#HIDDEN}.
     * <p>
     * A missing tag counts as "not hidden", which is why every place that installs
     * {@link #slenderRule(Player)} has to set the tag first.
     *
     * @param player the player to check
     * @return {@code true} if the player carries {@link SlenderBarHelper#HIDDEN}
     */
    public static boolean isHidden(Player player) {
        return player.hasTag(Tags.HIDDEN) && player.getTag(Tags.HIDDEN) == SlenderBarHelper.HIDDEN;
    }

    /**
     * Re-evaluates the viewable rule of the given player and of every other online player.
     * <p>
     * The second part is not redundant: {@link #spectatorRule()} tests the <i>viewer</i>, so whenever a
     * player changes role the rules of everybody else have to be re-run to pull that player in or out of
     * their viewer set. Re-evaluating a player without an installed rule is a no-op for anybody who is
     * already a viewer.
     *
     * @param player the player whose role or hidden state just changed
     */
    public static void refresh(Player player) {
        player.updateViewableRule();
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.getUuid().equals(player.getUuid())) continue;
            onlinePlayer.updateViewableRule();
        }
    }

    private VisibilityRules() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
