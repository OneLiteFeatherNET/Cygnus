package net.onelitefeather.cygnus.vitality;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.RegenerationConfig;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationServiceTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-9;
    /** What a calm survivor regains in one tick with the defaults. */
    private static final double PER_TICK = 4.0D * RegenerationService.TICK_MILLIS / 1000.0D;

    private double fear;
    private Set<Player> survivors = new HashSet<>();

    private RegenerationService started(Env env) {
        RegenerationService service = new RegenerationService(RegenerationConfig.DEFAULT, id -> this.fear,
                () -> this.survivors, TickSections.NONE);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        return service;
    }

    private CygnusPlayer survivor(Env env, Instance instance) {
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        player.vitality().setUp(0);
        this.survivors.add(player);
        return player;
    }

    private static void hit(CygnusPlayer survivor, double points) {
        survivor.vitality().damage(points, survivor.getPosition());
    }

    /** Moves the healing on by the given number of steps. */
    private static void ticks(RegenerationService service, int steps) {
        for (int i = 0; i < steps; i++) {
            service.tick();
        }
    }

    private static boolean holdsPainkillers(Player player) {
        Byte tag = player.getInventory().getItemStack(Items.HEAL_SLOT).getTag(Tags.ITEM_TAG);
        return tag != null && tag == Items.HEAL_ITEM;
    }

    @Test
    @DisplayName("Every survivor gets the painkillers when the round starts")
    void handedOutAtStart(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);

        started(env);

        assertTrue(holdsPainkillers(survivor));
    }

    @Test
    @DisplayName("Taking them uses them up and heals at most 40 % of the maximum in a small round")
    void healsUpToTheShare(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D);

        assertTrue(service.use(survivor));
        ticks(service, 270);

        assertFalse(holdsPainkillers(survivor));
        assertEquals(80.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("They never heal more than is missing")
    void neverMoreThanMissing(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 10.0D);

        service.use(survivor);
        ticks(service, 270);

        assertEquals(100.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("A bigger round heals a smaller share")
    void biggerRoundSmallerShare(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        for (int i = 0; i < 3; i++) {
            survivor(env, instance);
        }
        RegenerationService service = started(env);
        hit(survivor, 60.0D);

        service.use(survivor);
        ticks(service, 270);

        assertEquals(70.0D, survivor.vitality().points(), EPSILON, "four survivors get 30 % back");
    }

    @Test
    @DisplayName("They can be taken right after a hit")
    void takenRightAfterAHit(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 20.0D);

        assertTrue(service.use(survivor));
    }

    @Test
    @DisplayName("They can not be taken at full health or for a scratch, and stay in the inventory")
    void refusedWhenFullOrScratched(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer full = survivor(env, instance);
        CygnusPlayer scratched = survivor(env, instance);
        RegenerationService service = started(env);
        hit(scratched, 5.0D);

        assertFalse(service.use(full), "nothing missing");
        assertFalse(service.use(scratched), "half a heart is less than the minimum");

        assertTrue(holdsPainkillers(full));
        assertTrue(holdsPainkillers(scratched));
    }

    @Test
    @DisplayName("They work only once per round")
    void onlyOnce(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D);
        service.use(survivor);
        ticks(service, 270);
        hit(survivor, 20.0D);

        assertFalse(service.use(survivor));
    }

    @Test
    @DisplayName("A hit costs only its health, the healing goes on in full")
    void hitKeepsTheRest(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D);
        service.use(survivor);
        ticks(service, 1);
        hit(survivor, 5.0D);

        ticks(service, 269);

        // 40 to come, all of it healed, the hit costs only its 5 health
        assertEquals(40.0D + 40.0D - 5.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("A hit bigger than the rest does not end the healing")
    void bigHitKeepsTheRest(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 20.0D);
        service.use(survivor);
        ticks(service, 1);
        hit(survivor, 25.0D);

        ticks(service, 269);

        assertEquals(80.0D + 20.0D - 25.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Fear slows the healing down")
    void fearSlowsItDown(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        this.fear = 0.5D;
        hit(survivor, 60.0D);

        service.use(survivor);
        ticks(service, 1);

        assertEquals(40.0D + PER_TICK / 2.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Spectators can not take them")
    void spectatorsCanNot(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D);
        survivor.setGameMode(GameMode.SPECTATOR);

        assertFalse(service.use(survivor));
    }
}
