package net.onelitefeather.cygnus.creek;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The survivors of one step: the players and a snapshot of each of them.
 * <p>
 * It is taken once per step and shared by every creek. Where a survivor stands and how scared they
 * are is the same for all of them. Only whether they see a creek, and whether a block stands in
 * between, differs. So every view here says {@code false} for both and each creek fills them in for
 * itself.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class SurvivorSnapshot {

    private final List<Player> players;
    private final List<SurvivorView> views;

    /**
     * Pairs up the players with their views.
     *
     * @param players the survivors
     * @param views   one view per survivor, in the same order
     */
    SurvivorSnapshot(List<Player> players, List<SurvivorView> views) {
        if (players.size() != views.size()) {
            throw new IllegalArgumentException("Every survivor needs exactly one view");
        }
        this.players = List.copyOf(players);
        this.views = List.copyOf(views);
    }

    /**
     * Takes a snapshot of every survivor.
     *
     * @param players the survivors of the round
     * @param dread   rates how scared each survivor is
     * @return the snapshot
     */
    static SurvivorSnapshot take(Collection<Player> players, DreadSource dread) {
        List<Player> survivors = List.copyOf(players);
        List<SurvivorView> views = new ArrayList<>(survivors.size());
        for (Player survivor : survivors) {
            views.add(new SurvivorView(survivor.getUuid(), survivor.getPosition(),
                    dread.dreadOf(survivor.getUuid()), false, false));
        }
        return new SurvivorSnapshot(survivors, views);
    }

    /**
     * The survivors.
     *
     * @return the players, in the same order as {@link #views()}
     */
    List<Player> players() {
        return this.players;
    }

    /**
     * One view per survivor, none of them seeing a creek or having it in sight.
     *
     * @return the views, in the same order as {@link #players()}
     */
    List<SurvivorView> views() {
        return this.views;
    }

    /**
     * Looks up a survivor by id.
     *
     * @param id the id to look for
     * @return the player, or {@code null} if they are not among the survivors
     */
    @Nullable Player player(UUID id) {
        for (Player player : this.players) {
            if (player.getUuid().equals(id)) return player;
        }
        return null;
    }

    /**
     * How many survivors there are.
     *
     * @return the count
     */
    int size() {
        return this.players.size();
    }
}
