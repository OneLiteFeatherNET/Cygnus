package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.monster.CreakingMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreakingBodyIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("He is shown to the chosen viewers and nobody else")
    void showsHimOnlyToTheChosen(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(2, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));

        assertTrue(body.entity().getViewers().isEmpty(), "nobody sees him before he is shown");

        body.showTo(Set.of(first.getUuid()));

        assertTrue(body.entity().getViewers().contains(first));
        assertFalse(body.entity().getViewers().contains(second));
        assertTrue(body.isVisibleTo(first.getUuid()));

        body.showTo(Set.of());

        assertTrue(body.entity().getViewers().isEmpty());
    }

    @Test
    @DisplayName("The hunting look lights his eyes and freezing stops him")
    void looksFollowTheState(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        CreakingMeta meta = (CreakingMeta) body.entity().getEntityMeta();

        body.setAggressive(true);
        assertTrue(meta.isActive());

        body.setFrozen(true);
        assertFalse(meta.canMove());

        body.setFrozen(false);
        assertTrue(meta.canMove());
    }

    @Test
    @DisplayName("Removing takes him out of the world")
    void removeTakesHimOut(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));

        body.remove();

        assertTrue(body.entity().isRemoved());
    }

    @Test
    @DisplayName("A goal the navigator turned down does not block the next one")
    void rejectedGoalDoesNotBlockTheNext(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40, 10.5));

        // Same block as he stands in: the navigator has nothing to do and turns it down.
        body.moveTo(new Pos(0.7, 40, 10.7), 0.25D);
        Pos next = new Pos(1.4, 40, 10.9);
        body.moveTo(next, 0.25D);

        assertEquals(next, ((EntityCreature) body.entity()).getNavigator().getGoalPosition());
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:leaf_litter", "minecraft:short_grass", "minecraft:fern"})
    @DisplayName("He walks through plants on the ground")
    void walksThroughPlants(String plant, Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        Block cover = Block.fromKey(plant);
        for (int x = -2; x <= 14; x++) {
            for (int z = 5; z <= 15; z++) instance.setBlock(x, 40, z, cover);
        }
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40, 10.5));
        Pos goal = new Pos(10.5, 40, 10.5);

        walk(env, body, goal);

        assertTrue(body.position().distance(goal) <= 1.0D, "he gets through the " + plant + ", stands at " + body.position());
    }

    @Test
    @DisplayName("He walks along a row of slabs without hopping")
    void walksOverSlabsWithoutHopping(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        for (int x = 0; x <= 12; x++) {
            instance.setBlock(x, 40, 10, Block.STONE_SLAB);
        }
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40.5, 10.5));
        Pos goal = new Pos(10.5, 40.5, 10.5);

        double highest = walk(env, body, goal);

        assertTrue(body.position().distance(goal) <= 1.0D, "he reaches the end of the slabs, stands at " + body.position());
        assertTrue(highest < 41.0D, "he does not jump on flat slabs, highest y " + highest);
    }

    @Test
    @DisplayName("He climbs steps made of slabs")
    void climbsSlabSteps(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        // floor 40 -> slab 40.5 -> block 41 -> slab 41.5 -> block 42
        // across the whole width, so he cannot walk around them
        for (int z = 0; z <= 20; z++) {
            instance.setBlock(3, 40, z, Block.STONE_SLAB);
            for (int x = 4; x <= 20; x++) instance.setBlock(x, 40, z, Block.STONE);
            instance.setBlock(5, 41, z, Block.STONE_SLAB);
            for (int x = 6; x <= 20; x++) instance.setBlock(x, 41, z, Block.STONE);
        }
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40, 10.5));
        Pos goal = new Pos(10.5, 42, 10.5);

        walk(env, body, goal);

        assertTrue(body.position().distance(goal) <= 1.0D, "he gets to the top, stands at " + body.position());
    }

    @Test
    @DisplayName("The step he walks up comes from his step height attribute")
    void stepHeightFollowsTheAttribute(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.5, 40, 0.5));
        EntityCreature creature = (EntityCreature) body.entity();
        StepFollower follower = new StepFollower(creature);
        assertEquals(CreakingBody.STEP_HEIGHT, follower.stepHeight(), 1.0E-9, "a vanilla mob's step, not a creaking's");

        creature.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.0D);

        assertEquals(1.0D, follower.stepHeight(), 1.0E-9);
    }

    @Test
    @DisplayName("He drops into a gap only one block wide")
    void dropsIntoANarrowGap(Env env) {
        Instance instance = env.createFlatInstance();
        env.createConnection().connect(instance, new Pos(0, 40, 0));
        for (int x = -2; x <= 12; x++) {
            for (int z = 5; z <= 15; z++) instance.setBlock(x, 40, z, Block.STONE);
        }
        instance.setBlock(8, 40, 10, Block.AIR);
        // off the block grid, so full steps never land him exactly in the middle of the gap
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0.6, 41, 10.37));
        Pos goal = new Pos(8.5, 40, 10.5);

        walk(env, body, goal);

        assertTrue(body.position().distance(goal) <= 1.0D, "he gets down into the gap, stands at " + body.position());
    }

    /**
     * Lets him settle, sends him to the goal and ticks until he is there or 10 seconds are over.
     *
     * @return the highest y he reached on the way
     */
    private static double walk(Env env, CreakingBody body, Pos goal) {
        for (int i = 0; i < 20; i++) env.tick();
        body.moveTo(goal, 0.25D);
        double highest = body.position().y();
        for (int i = 0; i < 200 && body.position().distance(goal) > 1.0D; i++) {
            env.tick();
            highest = Math.max(highest, body.position().y());
        }
        return highest;
    }
}
