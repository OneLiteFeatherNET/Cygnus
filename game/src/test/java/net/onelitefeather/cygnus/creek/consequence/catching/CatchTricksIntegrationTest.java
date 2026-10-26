package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchTricksIntegrationTest extends CygnusPlayerTestBase {

    private static final double HEIGHT = 5.0D;

    private static final Pos SPAWN = new Pos(30, 40, -20);

    private static CatchTricks tricks(CatchConsequence base, double swapChance, Set<Player> survivors) {
        return tricks(base, swapChance, survivors, null);
    }

    private static CatchTricks tricks(CatchConsequence base, double swapChance, Set<Player> survivors,
                                      @Nullable Pos spawn) {
        RandomGenerator random = new java.util.Random(7L);
        return new CatchTricks(base, new CatchSwap(() -> survivors, random), new CatchSpawnTeleport(() -> spawn),
                new CatchLaunch(HEIGHT), swapChance, random);
    }

    @Test
    @DisplayName("A swap chance of 0 always throws the caught survivor")
    void chanceZeroLaunches(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0));

        tricks(_ -> {
        }, 0.0D, Set.of(caught, other)).apply(caught);
        env.tick();

        assertEquals(new Vec(10, 40, 0), other.getPosition().asVec(), "nobody was swapped");
        assertTrue(caught.getVelocity().y() > 0.0D, "the survivor was thrown");
    }

    @Test
    @DisplayName("A swap chance of 1 swaps with the only other survivor when there is no slender spawn")
    void chanceOneSwaps(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0));

        tricks(_ -> {
        }, 1.0D, Set.of(caught, other)).apply(caught);
        assertEquals(Vec.ZERO, caught.getVelocity());
        assertEquals(Vec.ZERO, other.getVelocity());
        env.tick();

        assertEquals(new Vec(10, 40, 0), caught.getPosition().asVec());
        assertEquals(new Vec(0, 40, 0), other.getPosition().asVec());
    }

    @Test
    @DisplayName("A teleport without a partner and without a slender spawn falls back to the throw")
    void noPartnerLaunches(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));

        tricks(_ -> {
        }, 1.0D, Set.of(caught)).apply(caught);

        assertTrue(caught.getVelocity().y() > 0.0D, "a catch always does something");
    }

    @Test
    @DisplayName("A swap without a partner falls back to the slender spawn")
    void noPartnerTeleportsToSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));

        tricks(_ -> {
        }, 1.0D, Set.of(caught), SPAWN).apply(caught);
        env.tick();

        assertEquals(SPAWN.asVec(), caught.getPosition().asVec());
    }

    @Test
    @DisplayName("A teleport chooses between the swap and the slender spawn")
    void teleportUsesBoth(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0));
        Set<Vec> landings = new HashSet<>();
        CatchTricks tricks = tricks(_ -> {
        }, 1.0D, Set.of(caught, other), SPAWN);

        for (int i = 0; i < 20; i++) {
            caught.teleport(new Pos(0, 40, 0)).join();
            other.teleport(new Pos(10, 40, 0)).join();
            tricks.apply(caught);
            env.tick();
            landings.add(caught.getPosition().asVec());
        }

        assertEquals(Set.of(new Vec(10, 40, 0), SPAWN.asVec()), landings);
    }

    @Test
    @DisplayName("The base effects run first and for either trick")
    void baseEffectsAlwaysRun(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(10, 40, 0));
        List<Pos> positionWhenApplied = new ArrayList<>();

        for (double chance : new double[]{0.0D, 1.0D}) {
            tricks(player -> positionWhenApplied.add(player.getPosition()), chance, Set.of(caught, other)).apply(caught);
        }

        assertEquals(2, positionWhenApplied.size(), "both catches applied the base effects");
        assertEquals(new Pos(0, 40, 0), positionWhenApplied.get(0));
    }

    @Test
    @DisplayName("Cleaning up is passed on to the base consequence")
    void cleanUpIsPassedOn(Env env) {
        boolean[] cleaned = {false};
        CatchConsequence base = new CatchConsequence() {
            @Override
            public void apply(Player survivor) {
            }

            @Override
            public void cleanUp() {
                cleaned[0] = true;
            }
        };

        tricks(base, 0.5D, Set.of()).cleanUp();

        assertTrue(cleaned[0]);
    }
}
