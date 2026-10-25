package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class LureSpotFinderIntegrationTest {

    /** Standing on the flat floor, looking south. */
    private static final Pos SURVIVOR = new Pos(0.5, 40, 0.5, 0, 0);

    @Test
    @DisplayName("The spot lies off screen and between the near and the far distance")
    void findsASpotOffScreenAndInRange(Env env) {
        Instance instance = flat(env);

        Pos spot = finder(instance).find(SURVIVOR, new Random(3)).orElseThrow();

        double distance = Math.hypot(spot.x() - SURVIVOR.x(), spot.z() - SURVIVOR.z());
        assertTrue(distance >= 14.5 && distance <= 25.5, "distance " + distance);
        double angle = offView(SURVIVOR, spot);
        assertTrue(angle >= 79.0 && angle <= 131.0, "angle " + angle);
    }

    @Test
    @DisplayName("Nobody stands in water")
    void skipsWater(Env env) {
        Instance instance = flat(env);
        ring(instance, 13, 27, 40, 40, Block.WATER);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("Nobody stands in lava")
    void skipsLava(Env env) {
        Instance instance = flat(env);
        ring(instance, 13, 27, 40, 40, Block.LAVA);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("Nobody stands more than three blocks higher than the survivor")
    void skipsSpotsTooHigh(Env env) {
        Instance instance = flat(env);
        ring(instance, 13, 27, 40, 43, Block.STONE);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("Nobody stands on a bush")
    void skipsLeaves(Env env) {
        Instance instance = flat(env);
        ring(instance, 13, 27, 40, 40, Block.OAK_LEAVES);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("Nobody stands on a fence")
    void skipsFences(Env env) {
        Instance instance = flat(env);
        ring(instance, 13, 27, 40, 40, Block.OAK_FENCE);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("Nobody stands behind a wall")
    void skipsSpotsBehindAWall(Env env) {
        Instance instance = flat(env);
        ring(instance, 10, 12, 40, 44, Block.STONE);

        assertEquals(Optional.empty(), finder(instance).find(SURVIVOR, new Random(3)));
    }

    @Test
    @DisplayName("The figure looks at the survivor")
    void figureLooksAtTheSurvivor(Env env) {
        Instance instance = flat(env);

        Pos spot = finder(instance).find(SURVIVOR, new Random(3)).orElseThrow();

        Pos expected = spot.add(0, SurvivorView.EYE_HEIGHT, 0).withLookAt(SURVIVOR.add(0, SurvivorView.EYE_HEIGHT, 0));
        assertEquals(expected.yaw(), spot.yaw(), 1.0F);
    }

    @Test
    @DisplayName("A line through air is clear, a line through stone is not")
    void clearLine(Env env) {
        Instance instance = flat(env);
        instance.setBlock(0, 41, 5, Block.STONE);

        assertTrue(LureSpotFinder.clearLine(instance, new Pos(0.5, 41.6, 0.5), new Pos(5.5, 41.6, 0.5)));
        assertTrue(!LureSpotFinder.clearLine(instance, new Pos(0.5, 41.6, 0.5), new Pos(0.5, 41.6, 10.5)));
    }

    private static LureSpotFinder finder(Instance instance) {
        return new LureSpotFinder(() -> instance);
    }

    private static Instance flat(Env env) {
        Instance instance = env.createFlatInstance();
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 2; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }

    /**
     * Fills every column whose distance to the survivor lies in [inner, outer) from minY to maxY.
     */
    private static void ring(Instance instance, int inner, int outer, int minY, int maxY, Block block) {
        for (int x = -outer - 1; x <= outer + 1; x++) {
            for (int z = -outer - 1; z <= outer + 1; z++) {
                double distance = Math.hypot(x + 0.5 - SURVIVOR.x(), z + 0.5 - SURVIVOR.z());
                if (distance < inner || distance >= outer) continue;
                for (int y = minY; y <= maxY; y++) {
                    instance.setBlock(x, y, z, block);
                }
            }
        }
    }

    /**
     * How far off the survivor's view the spot is, in degrees.
     */
    private static double offView(Pos survivor, Pos spot) {
        double radians = Math.toRadians(survivor.yaw());
        double lookX = -Math.sin(radians);
        double lookZ = Math.cos(radians);
        double toX = spot.x() - survivor.x();
        double toZ = spot.z() - survivor.z();
        double cos = (lookX * toX + lookZ * toZ) / Math.hypot(toX, toZ);
        return Math.toDegrees(Math.acos(Math.clamp(cos, -1.0D, 1.0D)));
    }
}
