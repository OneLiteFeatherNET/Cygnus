package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class FootprintGroundTest {

    /** The flat test instance is stone up to y 39, so a player stands at y 40. */
    private static Instance flat(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        return instance;
    }

    @Test
    @DisplayName("A full block with air above takes a print in that air block")
    void solidGround(Env env) {
        Instance instance = flat(env);

        Optional<BlockVec> spot = FootprintGround.spot(instance, new Pos(0.5, 40, 0.5));

        assertEquals(Optional.of(new BlockVec(0, 40, 0)), spot);
    }

    @Test
    @DisplayName("Feet a hair below the surface still count as standing on it")
    void floatingPointFeet(Env env) {
        Instance instance = flat(env);

        Optional<BlockVec> spot = FootprintGround.spot(instance, new Pos(0.5, 39.99999, 0.5));

        assertEquals(Optional.of(new BlockVec(0, 40, 0)), spot);
    }

    @Test
    @DisplayName("Carpet, snow layers, grass and flowers take no print")
    void decorations(Env env) {
        Instance instance = flat(env);
        instance.setBlock(0, 40, 0, Block.WHITE_CARPET);
        instance.setBlock(1, 40, 0, Block.SNOW);
        instance.setBlock(2, 40, 0, Block.SHORT_GRASS);
        instance.setBlock(3, 40, 0, Block.POPPY);

        assertTrue(FootprintGround.spot(instance, new Pos(0.5, 40.0625, 0.5)).isEmpty(), "carpet");
        assertTrue(FootprintGround.spot(instance, new Pos(1.5, 40.125, 0.5)).isEmpty(), "snow layer");
        assertTrue(FootprintGround.spot(instance, new Pos(2.5, 40, 0.5)).isEmpty(), "grass");
        assertTrue(FootprintGround.spot(instance, new Pos(3.5, 40, 0.5)).isEmpty(), "flower");
    }

    @Test
    @DisplayName("Slabs and stairs take no print, the fake block would replace them")
    void slabAndStairs(Env env) {
        Instance instance = flat(env);
        instance.setBlock(0, 40, 0, Block.STONE_SLAB);
        instance.setBlock(1, 40, 0, Block.STONE_STAIRS);

        assertTrue(FootprintGround.spot(instance, new Pos(0.5, 40.5, 0.5)).isEmpty(), "slab");
        assertTrue(FootprintGround.spot(instance, new Pos(1.5, 40.5, 0.5)).isEmpty(), "stairs");
    }

    @Test
    @DisplayName("Mid-jump the print goes onto the ground below")
    void midJump(Env env) {
        Instance instance = flat(env);

        Optional<BlockVec> spot = FootprintGround.spot(instance, new Pos(0.5, 41.5, 0.5));

        assertEquals(Optional.of(new BlockVec(0, 40, 0)), spot);
    }

    @Test
    @DisplayName("Too high above the ground there is no print")
    void tooHigh(Env env) {
        Instance instance = flat(env);

        assertTrue(FootprintGround.spot(instance, new Pos(0.5, 43.5, 0.5)).isEmpty());
    }

    @Test
    @DisplayName("An unloaded chunk takes no print")
    void unloadedChunk(Env env) {
        Instance instance = flat(env);

        assertTrue(FootprintGround.spot(instance, new Pos(10_000.5, 40, 10_000.5)).isEmpty());
    }
}
