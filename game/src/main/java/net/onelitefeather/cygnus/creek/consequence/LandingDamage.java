package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hurts a thrown survivor once, when they come down again.
 * <p>
 * Only the thrown survivors are watched, each by a task on their own scheduler that runs on the
 * tick thread, so there is no scan over everyone. The watch waits for the survivor to leave the
 * ground and then to touch it again. It ends without damage when the survivor dies, leaves, turns
 * into a spectator or is no survivor any more, when they do not land within the timeout, when they
 * are thrown again (the new throw replaces the watch) and when the round ends.
 * </p>
 * <p>
 * The damage never kills: at least 1 health point is left. Setting health bypasses Minestom's
 * damage event, so {@link PlayerDamagedEvent} is dispatched by hand, aimed at the landing spot,
 * as for every other hit in Cygnus.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class LandingDamage {

    /** How long a throw may take before the damage is forgotten, in ticks. */
    static final int TIMEOUT_TICKS = 100;

    /** The health a landing always leaves. */
    static final float MINIMUM_HEALTH = 1.0F;

    private final float damage;
    private final Map<Player, Task> watches = new ConcurrentHashMap<>();

    /**
     * Sets up the damage.
     *
     * @param damage health points lost on landing; 0 or less turns it off
     */
    public LandingDamage(double damage) {
        this.damage = (float) damage;
    }

    /**
     * Starts watching a thrown survivor.
     *
     * @param survivor the survivor who has just been thrown
     */
    public void watch(Player survivor) {
        if (this.damage <= 0.0F) return;
        stop(survivor);
        int[] ticks = {0};
        boolean[] airborne = {false};
        Task task = survivor.scheduler().submitTask(() -> {
            if (!stillValid(survivor) || ++ticks[0] > TIMEOUT_TICKS) {
                this.watches.remove(survivor);
                return TaskSchedule.stop();
            }
            if (!survivor.isOnGround()) {
                airborne[0] = true;
            } else if (airborne[0]) {
                this.watches.remove(survivor);
                hurt(survivor);
                return TaskSchedule.stop();
            }
            return TaskSchedule.nextTick();
        });
        this.watches.put(survivor, task);
    }

    /**
     * Forgets every watch, for the end of the round.
     */
    public void cleanUp() {
        for (Player player : List.copyOf(this.watches.keySet())) {
            stop(player);
        }
    }

    private void stop(Player survivor) {
        Task task = this.watches.remove(survivor);
        if (task != null) task.cancel();
    }

    private static boolean stillValid(Player survivor) {
        return survivor.isOnline()
                && !survivor.isDead()
                && survivor.getHealth() > 0
                && survivor.getGameMode() != GameMode.SPECTATOR
                && !survivor.getGameMode().invulnerable()
                && TeamHelper.isSurvivorTeam(survivor);
    }

    private void hurt(Player survivor) {
        float before = survivor.getHealth();
        float after = Math.max(MINIMUM_HEALTH, before - this.damage);
        float lost = before - after;
        if (lost <= 0.0F) return;
        survivor.setHealth(after);
        EventDispatcher.call(new PlayerDamagedEvent(survivor, survivor.getPosition(), lost));
    }
}
