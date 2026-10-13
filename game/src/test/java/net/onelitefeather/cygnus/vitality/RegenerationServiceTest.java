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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegenerationServiceTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-9;
    /** What a calm survivor regains in one tick with the defaults. */
    private static final double PER_TICK = 4.0D * RegenerationService.TICK_MILLIS / 1000.0D;

    private final AtomicLong clock = new AtomicLong();
    private double fear;
    private Set<Player> survivors = new HashSet<>();

    private RegenerationService started(Env env) {
        RegenerationService service = new RegenerationService(RegenerationConfig.DEFAULT, id -> this.fear,
                () -> this.survivors, this.clock::get, TickSections.NONE);
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

    private void hit(CygnusPlayer survivor, double points, long at) {
        this.clock.set(at);
        survivor.vitality().damage(points, survivor.getPosition());
    }

    private boolean use(RegenerationService service, Player survivor, long at) {
        this.clock.set(at);
        return service.use(survivor);
    }

    /** Ticks the service every 100 ms from one moment to another, both included. */
    private void ticks(RegenerationService service, long from, long to) {
        for (long now = from; now <= to; now += RegenerationService.TICK_MILLIS) {
            this.clock.set(now);
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
        hit(survivor, 60.0D, 0L);

        assertTrue(use(service, survivor, 3_000L));
        ticks(service, 3_100L, 30_000L);

        assertFalse(holdsPainkillers(survivor));
        assertEquals(80.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("They never heal more than is missing")
    void neverMoreThanMissing(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 10.0D, 0L);

        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 30_000L);

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
        hit(survivor, 60.0D, 0L);

        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 30_000L);

        assertEquals(70.0D, survivor.vitality().points(), EPSILON, "four survivors get 30 % back");
    }

    @Test
    @DisplayName("They can not be taken right after a hit or at full health, and stay in the inventory")
    void refusedWhenHurtOrFull(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer full = survivor(env, instance);
        CygnusPlayer hurt = survivor(env, instance);
        RegenerationService service = started(env);
        hit(hurt, 20.0D, 0L);

        assertFalse(use(service, hurt, 1_000L), "hit a second ago");
        assertFalse(use(service, full, 1_000L), "nothing missing");

        assertTrue(holdsPainkillers(hurt));
        assertTrue(holdsPainkillers(full));
    }

    @Test
    @DisplayName("They work only once per round")
    void onlyOnce(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D, 0L);
        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 30_000L);
        hit(survivor, 20.0D, 30_000L);

        assertFalse(use(service, survivor, 40_000L));
    }

    @Test
    @DisplayName("A hit takes its damage off what is still to come, and the healing goes on")
    void hitShrinksTheRest(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D, 0L);
        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 3_100L);
        hit(survivor, 5.0D, 3_150L);

        ticks(service, 3_200L, 30_000L);

        // 40 to come, one tick healed, then the hit costs 5 health and 5 of the rest
        assertEquals(40.0D + PER_TICK - 5.0D + (40.0D - PER_TICK - 5.0D), survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("A hit bigger than the rest ends the healing")
    void bigHitEndsTheHealing(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 20.0D, 0L);
        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 3_100L);
        hit(survivor, 25.0D, 3_150L);

        ticks(service, 3_200L, 30_000L);

        assertEquals(80.0D + PER_TICK - 25.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Fear slows the healing down")
    void fearSlowsItDown(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        this.fear = 0.5D;
        hit(survivor, 60.0D, 0L);

        use(service, survivor, 3_000L);
        ticks(service, 3_100L, 3_100L);

        assertEquals(40.0D + PER_TICK / 2.0D, survivor.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Spectators can not take them")
    void spectatorsCanNot(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = survivor(env, instance);
        RegenerationService service = started(env);
        hit(survivor, 60.0D, 0L);
        survivor.setGameMode(GameMode.SPECTATOR);

        assertFalse(use(service, survivor, 3_000L));
    }
}
