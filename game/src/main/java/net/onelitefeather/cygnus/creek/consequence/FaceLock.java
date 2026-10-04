package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Turns a player's head towards a point and holds it there, whatever they do with the mouse.
 * <p>
 * The head turns over {@link #TURN_TICKS} instead of snapping round, so it feels like being turned
 * rather than like a glitch. After that it is put back on the point every tick. The point is asked
 * for anew each tick, so a moving target is followed.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
final class FaceLock {

    /** How long turning the head takes, in ticks. */
    static final int TURN_TICKS = 5;

    private final Map<Player, Lock> running = new ConcurrentHashMap<>();

    /**
     * Turns the player's head towards the target and holds it there. A lock already on the player
     * is replaced.
     *
     * @param player the player
     * @param target where the head should point, asked for every tick
     * @param ticks  how long the head is held, in ticks
     */
    void lock(Player player, Supplier<? extends Point> target, int ticks) {
        this.release(player);
        Pos view = player.getPosition();
        Lock lock = new Lock(player, target, view.yaw(), view.pitch(), ticks);
        this.running.put(player, lock);
        lock.task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> this.step(lock))
                .delay(TaskSchedule.tick(1))
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }

    /**
     * Lets go of every head still held.
     */
    void cleanUp() {
        for (Player player : List.copyOf(this.running.keySet())) {
            this.release(player);
        }
    }

    private void step(Lock lock) {
        lock.elapsed++;
        Player player = lock.player;
        if (lock.elapsed > lock.ticks || !player.isOnline()) {
            this.release(player);
            return;
        }
        Pos eyes = player.getPosition().add(0, player.getEyeHeight(), 0);
        double progress = Math.min(1.0D, (double) lock.elapsed / TURN_TICKS);
        player.lookAt(lookPoint(eyes, lock.target.get(), lock.fromYaw, lock.fromPitch, progress));
    }

    private void release(Player player) {
        Lock lock = this.running.remove(player);
        if (lock != null && lock.task != null) lock.task.cancel();
    }

    /**
     * Works out the point to look at for a head part of the way from where it looked to the target.
     * It turns the short way round.
     *
     * @param eyes      where the eyes are
     * @param target    where the head should end up pointing
     * @param fromYaw   the yaw the head started at
     * @param fromPitch the pitch the head started at
     * @param progress  how far the turn is, from 0 (not at all) to 1 (on the target)
     * @return a point in the direction the head should point now
     */
    static Pos lookPoint(Pos eyes, Point target, float fromYaw, float fromPitch, double progress) {
        Vec towards = target.sub(eyes).asVec();
        // Worked out here rather than with PositionUtils, whose pitch is off for diagonal views.
        double toYaw = Math.toDegrees(Math.atan2(-towards.x(), towards.z()));
        double toPitch = Math.toDegrees(-Math.atan2(towards.y(), Math.hypot(towards.x(), towards.z())));
        float yaw = (float) (fromYaw + shortTurn(toYaw - fromYaw) * progress);
        float pitch = (float) (fromPitch + (toPitch - fromPitch) * progress);
        Vec direction = eyes.withView(yaw, pitch).direction();
        return eyes.add(direction.mul(Math.max(1.0D, towards.length())));
    }

    /**
     * Brings a turn into the range from -180 to 180 degrees, so it goes the short way round.
     */
    private static double shortTurn(double degrees) {
        double turn = degrees % 360.0D;
        if (turn >= 180.0D) return turn - 360.0D;
        if (turn < -180.0D) return turn + 360.0D;
        return turn;
    }

    private static final class Lock {

        private final Player player;
        private final Supplier<? extends Point> target;
        private final float fromYaw;
        private final float fromPitch;
        private final int ticks;
        private int elapsed;
        private @Nullable Task task;

        private Lock(Player player, Supplier<? extends Point> target, float fromYaw, float fromPitch, int ticks) {
            this.player = player;
            this.target = target;
            this.fromYaw = fromYaw;
            this.fromPitch = fromPitch;
            this.ticks = ticks;
        }
    }
}
