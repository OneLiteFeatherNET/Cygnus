package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.Ground;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Finds a spot where a living player could plausibly stand: on the ground, about as high as the
 * survivor, with nothing between them, just outside the survivor's view.
 * <p>
 * The creek's {@code SpotFinder} looks for the opposite, a spot nobody sees, so only the ground
 * search is shared with it.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class LureSpotFinder {

    /** How many candidate spots are tried per search. */
    static final int ATTEMPTS = 16;

    /** How far apart the samples along a line of sight are, in blocks. */
    static final double LINE_STEP = 0.2D;

    private final LureConfig config;
    private final Ground ground;
    private final Supplier<? extends @Nullable Instance> instance;

    /**
     * Sets up the search.
     *
     * @param config   the settings
     * @param ground   puts candidates onto a floor
     * @param instance supplies the round's instance, or {@code null} while there is none
     */
    public LureSpotFinder(LureConfig config, Ground ground, Supplier<? extends @Nullable Instance> instance) {
        this.config = config;
        this.ground = ground;
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
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double offset = random.nextDouble(this.config.minAngle(), this.config.maxAngle()) * (random.nextBoolean() ? 1.0D : -1.0D);
            double distance = random.nextDouble(this.config.minDistance(), this.config.maxDistance());
            Optional<Pos> spot = this.ground.settle(step(survivorFeet, survivorFeet.yaw() + offset, distance))
                    .filter(candidate -> this.fits(world, survivorFeet, eyes, candidate));
            if (spot.isPresent()) {
                Pos at = spot.get();
                Pos look = at.add(0, SurvivorView.EYE_HEIGHT, 0).withLookAt(eyes);
                return Optional.of(at.withView(look.yaw(), look.pitch()));
            }
        }
        return Optional.empty();
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
        if (Math.abs(spot.y() - survivorFeet.y()) > this.config.maxHeightOffset()) return false;
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

    private static Pos step(Pos from, double yaw, double distance) {
        double radians = Math.toRadians(yaw);
        // Minecraft's yaw goes clockwise from south (+Z), so the direction is (-sin, cos).
        return new Pos(from.x() - Math.sin(radians) * distance, from.y(), from.z() + Math.cos(radians) * distance);
    }
}
