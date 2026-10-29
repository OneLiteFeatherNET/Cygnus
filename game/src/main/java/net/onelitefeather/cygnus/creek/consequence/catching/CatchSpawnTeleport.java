package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Teleports the caught survivor to the spawn of the slender, far away from the own team.
 * <p>
 * Like the swap, only the coordinates change: the survivor keeps looking where they looked and
 * loses their velocity, then hears the teleport sound. The survivor stays in their instance.
 * </p>
 * <p>
 * Without a map or without a slender spawn nothing happens and {@link #perform(Player)} says so.
 * </p>
 * <p>
 * A survivor who was just sent to the spawn is not sent there again for {@link #COOLDOWN_MILLIS}.
 * The trickster teleports on every catch, and without the pause the same survivor could land next
 * to the slender over and over. During the pause {@link #perform(Player)} refuses, so the creek
 * falls back to another trick.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class CatchSpawnTeleport implements CatchTrick {

    /** How long a survivor is safe from another trip to the slender spawn, in milliseconds. */
    static final long COOLDOWN_MILLIS = 60_000L;

    /** Until when a survivor is safe from another trip to the slender spawn, in epoch milliseconds. */
    static final Tag<Long> COOLDOWN_UNTIL = Tag.Transient("creekSpawnTeleportUntil");

    private final Supplier<@Nullable Pos> slenderSpawn;

    /**
     * Sets up the teleport.
     *
     * @param slenderSpawn supplies the slender spawn of the current map, {@code null} without one
     */
    public CatchSpawnTeleport(Supplier<@Nullable Pos> slenderSpawn) {
        this.slenderSpawn = slenderSpawn;
    }

    @Override
    public boolean perform(Player caught) {
        if (caught.getInstance() == null) return false;
        long now = System.currentTimeMillis();
        Long cooldown = caught.getTag(COOLDOWN_UNTIL);
        if (cooldown != null && now < cooldown) return false;
        Pos spawn = this.slenderSpawn.get();
        if (spawn == null) return false;

        Pos from = caught.getPosition();
        caught.teleport(spawn.withView(from.yaw(), from.pitch()));
        caught.setVelocity(Vec.ZERO);
        caught.playSound(SlenderBarHelper.TELEPORT);
        caught.setTag(COOLDOWN_UNTIL, now + COOLDOWN_MILLIS);
        return true;
    }
}
