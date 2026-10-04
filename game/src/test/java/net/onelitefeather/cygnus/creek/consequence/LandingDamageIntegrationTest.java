package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingDamageIntegrationTest extends CygnusPlayerTestBase {

    private static Player survivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        player.setHealth(20.0F);
        return player;
    }

    private static void throwAndLand(Env env, Player player) {
        player.refreshOnGround(false);
        env.tick();
        player.refreshOnGround(true);
        env.tick();
    }

    @Test
    @DisplayName("A survivor who lands after the throw loses the configured health, once")
    void damagesOnceOnLanding(Env env) {
        Player player = survivor(env);
        List<Float> hits = new ArrayList<>();
        env.process().eventHandler().addListener(PlayerDamagedEvent.class, event -> hits.add(event.getAmount()));
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(true);

        landing.watch(player);
        throwAndLand(env, player);
        player.refreshOnGround(false);
        env.tick();
        player.refreshOnGround(true);
        env.tick();

        assertEquals(16.0F, player.getHealth(), 0.0F);
        assertEquals(List.of(4.0F), hits, "the damage event fired once");
    }

    @Test
    @DisplayName("A landing never takes the survivor below 1 health")
    void neverKills(Env env) {
        Player player = survivor(env);
        player.setHealth(3.0F);
        LandingDamage landing = new LandingDamage(20.0D);
        player.refreshOnGround(true);

        landing.watch(player);
        throwAndLand(env, player);

        assertEquals(1.0F, player.getHealth(), 0.0F);
        assertTrue(!player.isDead());
    }

    @Test
    @DisplayName("A damage of 0 turns the landing damage off")
    void zeroDisables(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(0.0D);
        player.refreshOnGround(true);

        landing.watch(player);
        throwAndLand(env, player);

        assertEquals(20.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("Nothing happens to a survivor who has not left the ground yet")
    void noDamageWithoutLeavingTheGround(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(true);

        landing.watch(player);
        env.tick();
        env.tick();

        assertEquals(20.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("A survivor who does not land within the timeout takes no damage")
    void timesOut(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(false);

        landing.watch(player);
        for (int i = 0; i < LandingDamage.TIMEOUT_TICKS + 5; i++) env.tick();
        player.refreshOnGround(true);
        env.tick();

        assertEquals(20.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("A survivor who turns into a spectator in the air takes no damage")
    void spectatorTakesNoDamage(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(false);

        landing.watch(player);
        env.tick();
        player.setGameMode(GameMode.SPECTATOR);
        player.refreshOnGround(true);
        env.tick();

        assertEquals(20.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("A survivor who dies in the air takes no further damage")
    void deadTakesNoDamage(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(false);
        landing.watch(player);
        env.tick();

        player.kill();
        player.refreshOnGround(true);
        env.tick();

        assertEquals(0.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("Cleaning up at the end of the round drops the watch")
    void cleanUpDropsTheWatch(Env env) {
        Player player = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        player.refreshOnGround(false);
        landing.watch(player);
        env.tick();

        landing.cleanUp();
        player.refreshOnGround(true);
        env.tick();

        assertEquals(20.0F, player.getHealth(), 0.0F);
    }

    @Test
    @DisplayName("A throw starts the watch and a swap does not")
    void onlyTheThrowIsWatched(Env env) {
        Player thrown = survivor(env);
        LandingDamage landing = new LandingDamage(4.0D);
        thrown.refreshOnGround(true);

        new CatchLaunch(5.0D, landing).perform(thrown);
        throwAndLand(env, thrown);

        assertEquals(16.0F, thrown.getHealth(), 0.0F);
    }
}
