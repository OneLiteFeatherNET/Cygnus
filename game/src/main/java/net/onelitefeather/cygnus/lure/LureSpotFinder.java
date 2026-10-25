package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.BesideSearch;
import net.onelitefeather.cygnus.creek.world.Ground;
import net.onelitefeather.cygnus.creek.world.InstanceGround;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Finds a spot where a living player could plausibly stand: on the ground, about as high as the
 * survivor, with nothing between them, just outside the survivor's view.
 * <p>
 * The creek's {@code SpotFinder} looks for the opposite, a spot nobody sees, so only the search
 * beside the view and the ground are shared with it.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureSpotFinder {

    /** How far apart the samples along a line of sight are, in blocks. */
    static final double LINE_STEP = 0.2D;

    /** How close the figure stands at the least, in blocks. */
    static final double MIN_DISTANCE = 15.0D;

    /** How far away the figure stands at the most, in blocks. */
    static final double MAX_DISTANCE = 25.0D;

    /**
     * How far off the view the figure stands at the least, in degrees. Above
     * {@link CreekConfig#HIDE_ANGLE} the survivor never sees it pop in.
     */
    static final double MIN_ANGLE = 80.0D;

    /** How far off the view the figure stands at the most, in degrees. */
    static final double MAX_ANGLE = 130.0D;

    /** How much higher or lower than the survivor the figure may stand, in blocks. */
    static final double MAX_HEIGHT_OFFSET = 3.0D;

    private final Ground ground;
    private final Supplier<? extends @Nullable Instance> instance;

    /**
     * Sets up the search on the floor of the round's instance.
     *
     * @param instance supplies the round's instance, or {@code null} while there is none
     */
    public LureSpotFinder(Supplier<? extends @Nullable Instance> instance) {
        this.ground = new InstanceGround(instance);
        this.instance = instance;
    }

    /**
     * Finds a spot next to a survivor.
     *
     * @param survivorFeet where the survivor's feet are, their yaw is where they look
     * @param random       the random source
     * @return the spot, looking at the survivor's eyes, or empty if no candidate worked
     */
    public Optional<Pos> find(Pos survivorFeet, RandomGenerator random) {
        Instance world = this.instance.get();
        if (world == null) return Optional.empty();
        Pos eyes = survivorFeet.add(0, SurvivorView.EYE_HEIGHT, 0);
        return BesideSearch.find(survivorFeet, MIN_DISTANCE, MAX_DISTANCE, MIN_ANGLE, MAX_ANGLE,
                candidate -> this.ground.settle(candidate).filter(spot -> this.fits(world, survivorFeet, eyes, spot)),
                random).map(at -> {
                    Pos look = at.add(0, SurvivorView.EYE_HEIGHT, 0).withLookAt(eyes);
                    return at.withView(look.yaw(), look.pitch());
                });
    }

    /**
     * Checks that nothing is in the way along a line. Blocks at the very ends do not count.
     *
     * @param world the blocks to check
     * @param from  where the line starts
     * @param to    where the line ends
     * @return {@code true} if no solid block lies on the line
     */
    static boolean clearLine(Block.Getter world, Point from, Point to) {
        Vec direction = new Vec(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
        double length = direction.length();
        if (length == 0.0D) return true;
        Vec unit = direction.div(length);
        for (double travelled = LINE_STEP; travelled < length; travelled += LINE_STEP) {
            if (world.getBlock(from.add(unit.mul(travelled))).solid()) return false;
        }
        return true;
    }

    private boolean fits(Instance world, Pos survivorFeet, Pos eyes, Pos spot) {
        if (Math.abs(spot.y() - survivorFeet.y()) > MAX_HEIGHT_OFFSET) return false;
        // The ground search only wants a solid floor and room above it. Water and lava pass that.
        Block floor = world.getBlock(spot.sub(0, 1, 0));
        if (world.getBlock(spot).liquid() || floor.liquid()) return false;
        if (!walkable(floor)) return false;
        return clearLine(world, eyes, spot.add(0, SurvivorView.EYE_HEIGHT, 0));
    }

    /**
     * Tells whether a living player would stand on a block. Nobody stands on top of a bush, a fence
     * or a wall, even though they are solid.
     */
    static boolean walkable(Block floor) {
        String name = floor.key().value();
        return !name.endsWith("_leaves") && !name.endsWith("_fence") && !name.endsWith("_fence_gate")
                && !name.endsWith("_wall");
    }
}
