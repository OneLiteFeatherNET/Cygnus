package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.Tags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchSwapIntegrationTest extends CygnusPlayerTestBase {

    private static final RandomGenerator SEEDED = new java.util.Random(42L);

    private static CatchSwap swapOf(Player... survivors) {
        return new CatchSwap(() -> Set.of(survivors), new java.util.Random(42L));
    }

    @Test
    @DisplayName("Two survivors exchange their coordinates and lose their velocity")
    void exchangesPositions(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 50, 20));
        caught.setVelocity(new Vec(1, 2, 3));
        other.setVelocity(new Vec(4, 5, 6));

        assertTrue(swapOf(caught, other).perform(caught));
        assertEquals(Vec.ZERO, caught.getVelocity());
        assertEquals(Vec.ZERO, other.getVelocity());
        // gravity acts again from the next tick on, so the velocity is read before it
        env.tick();

        assertEquals(new Vec(10, 50, 20), caught.getPosition().asVec());
        assertEquals(new Vec(0, 40, 0), other.getPosition().asVec());
    }

    @Test
    @DisplayName("Each survivor keeps their own view direction")
    void keepsTheView(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0, 90, 10));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0, -45, -20));

        swapOf(caught, other).perform(caught);
        env.tick();

        assertEquals(90.0F, caught.getPosition().yaw());
        assertEquals(10.0F, caught.getPosition().pitch());
        assertEquals(-45.0F, other.getPosition().yaw());
        assertEquals(-20.0F, other.getPosition().pitch());
    }

    @Test
    @DisplayName("Without another survivor the swap does not happen")
    void noPartner(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));

        assertFalse(swapOf(caught).perform(caught));
        env.tick();

        assertEquals(new Vec(0, 40, 0), caught.getPosition().asVec());
    }

    @Test
    @DisplayName("Spectators, the slender and survivors in another instance are never partners")
    void excludesNonPartners(Env env) {
        Instance instance = env.createFlatInstance();
        Instance elsewhere = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player spectatorTeam = env.createConnection().connect(instance, new Pos(1, 40, 0));
        spectatorTeam.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        Player spectatorMode = env.createConnection().connect(instance, new Pos(2, 40, 0));
        spectatorMode.setGameMode(GameMode.SPECTATOR);
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Player faraway = env.createConnection().connect(elsewhere, new Pos(4, 40, 0));

        assertFalse(swapOf(caught, spectatorTeam, spectatorMode, slender, faraway).perform(caught));
    }

    @Test
    @DisplayName("A real partner is chosen among those who qualify")
    void choosesAmongQualified(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Player partner = env.createConnection().connect(instance, new Pos(7, 40, 7));

        assertTrue(new CatchSwap(() -> Set.of(caught, slender, partner), SEEDED).perform(caught));
        env.tick();

        assertEquals(new Vec(7, 40, 7), caught.getPosition().asVec());
        assertEquals(new Vec(3, 40, 0), slender.getPosition().asVec(), "the slender stays put");
    }
}
