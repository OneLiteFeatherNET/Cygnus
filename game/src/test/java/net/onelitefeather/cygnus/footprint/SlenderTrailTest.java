package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlenderTrailTest extends CygnusPlayerTestBase {

    /** Default values, but a one tick delay so the tests stay short. */
    private static final FootprintConfig CONFIG = new FootprintConfig(3.0D, 0.25D, 50, 50, 12,
            2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D);

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    private static Player slender(Env env, Instance instance, byte hidden) {
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, hidden);
        return slender;
    }

    private static void step(SlenderTrail trail, Player slender) {
        trail.moved(slender, new Pos(0.5, 40, 0.5), new Pos(3.5, 40, 0.5));
    }

    @Test
    @DisplayName("A hidden slender leaves a print that only survivors see")
    void leavesPrint(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env, instance, SlenderBarHelper.HIDDEN);
        Player survivor = env.createPlayer(instance, new Pos(5, 40, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Player spectator = env.createPlayer(instance, new Pos(6, 40, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        FootprintSpawner spawner = new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
        SlenderTrail trail = new SlenderTrail(CONFIG, new FixedRandom(0.0D), spawner);

        step(trail, slender);
        tick(env, 4);

        assertEquals(1, spawner.live().size());
        Footprint footprint = spawner.live().iterator().next();
        assertTrue(footprint.isShownTo(survivor));
        assertFalse(footprint.isShownTo(spectator));
        assertFalse(footprint.isShownTo(slender));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A failed roll leaves nothing")
    void failedRoll(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env, instance, SlenderBarHelper.HIDDEN);
        FootprintSpawner spawner = new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
        SlenderTrail trail = new SlenderTrail(CONFIG, new FixedRandom(0.99D), spawner);

        step(trail, slender);
        tick(env, 4);

        assertTrue(spawner.live().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A visible slender leaves nothing")
    void visible(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env, instance, SlenderBarHelper.VISIBLE);
        FootprintSpawner spawner = new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
        SlenderTrail trail = new SlenderTrail(CONFIG, new FixedRandom(0.0D), spawner);

        step(trail, slender);
        tick(env, 4);

        assertTrue(spawner.live().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Turning visible during the delay does not take the print back")
    void hiddenAtTheStep(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env, instance, SlenderBarHelper.HIDDEN);
        FootprintSpawner spawner = new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
        SlenderTrail trail = new SlenderTrail(CONFIG, new FixedRandom(0.0D), spawner);

        step(trail, slender);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        tick(env, 4);

        assertEquals(1, spawner.live().size());
        env.destroyInstance(instance, true);
    }
}
