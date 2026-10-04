package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Swaps the caught survivor with another random survivor.
 * <p>
 * Only the coordinates change places: each survivor keeps looking where they looked, so the
 * swap does not spin anyone's camera. Both lose their velocity, so nobody keeps falling or flying
 * with the speed of the other spot. Both are moved in the same call on the tick thread, and both
 * hear the teleport sound.
 * </p>
 * <p>
 * The partner is a living survivor in the same instance. Spectators, the slender, players who are
 * gone and the caught survivor are never chosen. Without a partner nothing happens and
 * {@link #perform(Player)} says so.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CatchSwap implements CatchTrick {

    private final Supplier<Set<Player>> survivors;
    private final RandomGenerator random;

    /**
     * Sets up the swap.
     *
     * @param survivors supplies the survivors of the round
     * @param random    picks the partner
     */
    public CatchSwap(Supplier<Set<Player>> survivors, RandomGenerator random) {
        this.survivors = survivors;
        this.random = random;
    }

    @Override
    public boolean perform(Player caught) {
        if (caught.getInstance() == null) return false;
        // A set has no order, so sort for a partner choice that only depends on the random source.
        List<Player> partners = this.survivors.get().stream()
                .filter(player -> isPartner(caught, player))
                .sorted((a, b) -> a.getUuid().compareTo(b.getUuid()))
                .toList();
        if (partners.isEmpty()) return false;

        Player partner = partners.get(this.random.nextInt(partners.size()));
        Pos from = caught.getPosition();
        Pos to = partner.getPosition();
        caught.teleport(to.withView(from.yaw(), from.pitch()));
        partner.teleport(from.withView(to.yaw(), to.pitch()));
        caught.setVelocity(Vec.ZERO);
        partner.setVelocity(Vec.ZERO);
        Sound sound = SlenderBarHelper.TELEPORT;
        caught.playSound(sound);
        partner.playSound(sound);
        return true;
    }

    private static boolean isPartner(Player caught, Player other) {
        return other != caught
                && other.isOnline()
                && other.getInstance() == caught.getInstance()
                && other.getGameMode() != GameMode.SPECTATOR
                && !TeamHelper.isSlenderTeam(other)
                && !TeamHelper.isSpectatorTeam(other);
    }
}
