package net.onelitefeather.cygnus.footprint;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Places the prints, keeps one per block and kind, and fades and removes them once per second.
 * <p>
 * Every print gets a random facing, which is what keeps it from giving away a direction. A round
 * end or a leaving slender clears everything, so no fake block is left on any client.
 * </p>
 *
 * @author Joltra
 * @version 1.1.0
 * @since 2.17.0
 */
final class FootprintSpawner {

    /** The four ways a print can be turned. None of them says where the player went. */
    static final List<String> FACINGS = List.of("north", "east", "south", "west");

    private static final int TICKS_PER_SECOND = 20;

    private final RandomGenerator random;
    private final double fadeShare;
    private final Map<FootprintKind, Map<BlockVec, Footprint>> live = new EnumMap<>(FootprintKind.class);
    private final Set<Task> pending = ConcurrentHashMap.newKeySet();
    private @Nullable Task ticker;

    /**
     * Creates the spawner.
     *
     * @param random    the source for the facing of each print
     * @param fadeShare the share of the lifetime at its end that shows the faded stage
     */
    FootprintSpawner(RandomGenerator random, double fadeShare) {
        this.random = random;
        this.fadeShare = fadeShare;
        for (FootprintKind kind : FootprintKind.values()) {
            this.live.put(kind, new ConcurrentHashMap<>());
        }
    }

    /**
     * Places a print right away, if the ground allows one. A print of the same kind on the same
     * block is replaced.
     *
     * @param instance        the instance to place it in
     * @param position        where the player stood
     * @param kind            whose print it is
     * @param lifetimeSeconds how long it stays
     * @param viewers         who may see it
     * @return {@code true} if a print was placed
     */
    boolean spawn(Instance instance, Pos position, FootprintKind kind, int lifetimeSeconds, Predicate<Player> viewers) {
        Optional<BlockVec> spot = FootprintGround.spot(instance, position);
        if (spot.isEmpty()) return false;
        String facing = FACINGS.get(this.random.nextInt(0, FACINGS.size()));
        Footprint footprint = new Footprint(instance, spot.get(), kind, facing, lifetimeSeconds,
                this.fadeAfter(lifetimeSeconds), viewers);
        Footprint replaced = this.live.get(kind).put(spot.get(), footprint);
        // The old print has to go first, its real block would otherwise cover the new one.
        if (replaced != null) replaced.remove();
        footprint.refresh();
        this.startTicking();
        return true;
    }

    /**
     * Places a print after a delay. The ground is checked when the delay is over.
     *
     * @param delayTicks      the delay in ticks
     * @param instance        the instance to place it in
     * @param position        where the player stood
     * @param kind            whose print it is
     * @param lifetimeSeconds how long it stays
     * @param viewers         who may see it
     */
    void spawnLater(int delayTicks, Instance instance, Pos position, FootprintKind kind, int lifetimeSeconds,
                    Predicate<Player> viewers) {
        Task task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.spawn(instance, position, kind, lifetimeSeconds, viewers))
                .delay(TaskSchedule.tick(delayTicks))
                .schedule();
        this.pending.removeIf(running -> !running.isAlive());
        this.pending.add(task);
    }

    /**
     * Returns the prints that are still shown.
     *
     * @return the live prints of both kinds
     */
    List<Footprint> live() {
        List<Footprint> all = new ArrayList<>();
        for (Map<BlockVec, Footprint> prints : this.live.values()) {
            all.addAll(prints.values());
        }
        return all;
    }

    /**
     * Checks every print against its rule again, after a player changed team.
     */
    void refreshViewers() {
        for (Footprint footprint : this.live()) {
            footprint.refresh();
        }
    }

    /**
     * Sends the prints inside a chunk again to a player whose client just got that chunk. A chunk
     * sent to a client replaces every fake block in it.
     *
     * @param player the player
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     */
    void resend(Player player, int chunkX, int chunkZ) {
        for (Footprint footprint : this.live()) {
            BlockVec block = footprint.block();
            if (block.chunkX() == chunkX && block.chunkZ() == chunkZ) {
                footprint.resend(player);
            }
        }
    }

    /**
     * Drops a player who left from every print.
     *
     * @param player the player
     */
    void forget(Player player) {
        for (Footprint footprint : this.live()) {
            footprint.forget(player);
        }
    }

    /**
     * Removes every print, cancels the ones still waiting for their delay and stops the task.
     */
    synchronized void clear() {
        for (Task task : this.pending) {
            task.cancel();
        }
        this.pending.clear();
        if (this.ticker != null) {
            this.ticker.cancel();
            this.ticker = null;
        }
        for (Map<BlockVec, Footprint> prints : this.live.values()) {
            prints.values().forEach(Footprint::remove);
            prints.clear();
        }
    }

    private int fadeAfter(int lifetimeSeconds) {
        return lifetimeSeconds - (int) Math.round(lifetimeSeconds * this.fadeShare);
    }

    private synchronized void startTicking() {
        if (this.ticker != null) return;
        this.ticker = MinecraftServer.getSchedulerManager()
                .buildTask(this::second)
                .delay(TaskSchedule.tick(TICKS_PER_SECOND))
                .repeat(TaskSchedule.tick(TICKS_PER_SECOND))
                .schedule();
    }

    private void second() {
        for (Map<BlockVec, Footprint> prints : this.live.values()) {
            prints.values().removeIf(Footprint::second);
        }
    }
}
