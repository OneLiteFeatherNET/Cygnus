package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
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
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class CatchSpawnTeleport implements CatchTrick {

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
        Pos spawn = this.slenderSpawn.get();
        if (spawn == null) return false;

        Pos from = caught.getPosition();
        caught.teleport(spawn.withView(from.yaw(), from.pitch()));
        caught.setVelocity(Vec.ZERO);
        caught.playSound(SlenderBarHelper.TELEPORT);
        return true;
    }
}
