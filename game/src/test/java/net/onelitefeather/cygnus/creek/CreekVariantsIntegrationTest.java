package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.creek.state.HuntCooldowns;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.PatrolHelper;
import net.onelitefeather.cygnus.creek.consequence.StalkSounds;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.state.Contexts;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.Ground;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekVariantsIntegrationTest extends CygnusPlayerTestBase {

    private static final CreekConfig CONFIG = CreekConfig.DEFAULT;
    private static final CreekSight SIGHT = new CreekSight(CONFIG.sight().range(), CONFIG.sight().viewAngle());
    private static final CatchConsequence NO_CATCH = new CatchConsequence() {
        @Override
        public void apply(Player survivor) {
        }

        @Override
        public void cleanUp() {
        }
    };
    private static final PatrolHelper PATROL = new PatrolHelper(new Random(1));

    private static BiFunction<Pos, CreekState, Creek> spawner(Instance instance, CreekConfig config, SpotFinder spots) {
        CreekRound round = new CreekRound(SIGHT, spots, NO_CATCH, CreekWitness.NONE, PATROL,
                new StalkSounds(new Random(3)), config, new Random(3), HuntCooldowns.none());
        return (spot, initial) -> new Creek(CreakingBody.spawn(instance, spot), Contexts.route(), round, initial);
    }

    private static CreekVariants variants(Instance instance, CreekConfig config, Ground ground, long allowedAt) {
        SpotFinder spots = new SpotFinder(SIGHT, ground);
        return new CreekVariants(config, spots, new Random(5), allowedAt, spawner(instance, config, spots));
    }

    private static SurvivorView view(Player player, double dread) {
        return new SurvivorView(player.getUuid(), player.getPosition(), dread, false);
    }

    private static Player join(Env env, Instance instance, double x) {
        return env.createConnection().connect(instance, new Pos(x, 40, 0, 0, 0));
    }

    @Test
    @DisplayName("A variant starts for the survivor with the highest dread and only they see it")
    void startsForTheHighestDread(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        Player second = join(env, instance, 10);
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);
        List<Player> survivors = List.of(first, second);
        List<SurvivorView> views = List.of(view(first, 0.5D), view(second, 0.3D));

        variants.tick(new SurvivorSnapshot(survivors, views), 0L);
        variants.tick(new SurvivorSnapshot(survivors, views), 100L);

        assertEquals(1, variants.running().size(), "two survivors allow one variant");
        Creek variant = variants.running().get(first.getUuid());
        assertTrue(variant.body().isVisibleTo(first.getUuid()));
        assertFalse(variant.body().isVisibleTo(second.getUuid()));
    }

    @Test
    @DisplayName("Five survivors allow two variants, one per survivor")
    void twoVariantsForFiveSurvivors(Env env) {
        Instance instance = env.createFlatInstance();
        List<Player> survivors = List.of(join(env, instance, 0), join(env, instance, 100), join(env, instance, 200),
                join(env, instance, 300), join(env, instance, 400));
        List<SurvivorView> views = List.of(view(survivors.get(0), 0.5D), view(survivors.get(1), 0.4D),
                view(survivors.get(2), 0.3D), view(survivors.get(3), 0.0D), view(survivors.get(4), 0.0D));
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);

        variants.tick(new SurvivorSnapshot(survivors, views), 0L);
        variants.tick(new SurvivorSnapshot(survivors, views), 100L);

        assertEquals(2, variants.running().size());
        assertTrue(variants.running().containsKey(survivors.get(0).getUuid()));
        assertTrue(variants.running().containsKey(survivors.get(1).getUuid()));
    }

    @Test
    @DisplayName("Nothing starts before the allowed time")
    void waitsForTheAllowedTime(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 1000L);

        variants.tick(new SurvivorSnapshot(List.of(first), List.of(view(first, 0.9D))), 999L);

        assertTrue(variants.running().isEmpty());
    }

    @Test
    @DisplayName("After a variant ends, its target has a cooldown")
    void cooldownAfterTheEnd(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        Player second = join(env, instance, 10);
        List<Player> survivors = List.of(first, second);
        List<SurvivorView> views = List.of(view(first, 0.5D), view(second, 0.0D));
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);
        variants.tick(new SurvivorSnapshot(survivors, views), 0L);
        Creek variant = variants.running().get(first.getUuid());

        // the stalk lasts at most stalkMaxSeconds (90 s)
        variants.tick(new SurvivorSnapshot(survivors, views), 100_000L);
        assertTrue(variants.running().isEmpty());
        assertTrue(variant.body().entity().isRemoved());

        // dread 0.5 gives a 30 s cooldown with the default vanish times
        variants.tick(new SurvivorSnapshot(survivors, views), 129_999L);
        assertTrue(variants.running().isEmpty());

        variants.tick(new SurvivorSnapshot(survivors, views), 130_000L);
        assertEquals(1, variants.running().size());
    }

    @Test
    @DisplayName("A variant ends when its target leaves")
    void endsWhenTheTargetLeaves(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        Player second = join(env, instance, 10);
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);
        variants.tick(new SurvivorSnapshot(List.of(first, second), List.of(view(first, 0.5D), view(second, 0.0D))), 0L);
        Creek variant = variants.running().get(first.getUuid());

        variants.tick(new SurvivorSnapshot(List.of(second), List.of(view(second, 0.0D))), 100L);

        assertTrue(variants.running().isEmpty());
        assertTrue(variant.body().entity().isRemoved());
    }

    @Test
    @DisplayName("A dropping capacity does not cut a running variant short")
    void droppingCapacityKeepsRunningVariants(Env env) {
        Instance instance = env.createFlatInstance();
        List<Player> survivors = List.of(join(env, instance, 0), join(env, instance, 100), join(env, instance, 200),
                join(env, instance, 300), join(env, instance, 400));
        List<SurvivorView> views = List.of(view(survivors.get(0), 0.5D), view(survivors.get(1), 0.4D),
                view(survivors.get(2), 0.3D), view(survivors.get(3), 0.0D), view(survivors.get(4), 0.0D));
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);
        variants.tick(new SurvivorSnapshot(survivors, views), 0L);

        variants.tick(new SurvivorSnapshot(survivors.subList(0, 4), views.subList(0, 4)), 100L);

        assertEquals(2, variants.running().size());
    }

    @Test
    @DisplayName("Without a spot nothing starts, the next step tries again")
    void retriesWithoutASpot(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        AtomicBoolean floor = new AtomicBoolean(false);
        CreekVariants variants = variants(instance, CONFIG,
                candidate -> floor.get() ? Optional.of(candidate) : Optional.empty(), 0L);

        variants.tick(new SurvivorSnapshot(List.of(first), List.of(view(first, 0.5D))), 0L);
        assertTrue(variants.running().isEmpty());

        floor.set(true);
        variants.tick(new SurvivorSnapshot(List.of(first), List.of(view(first, 0.5D))), 100L);
        assertEquals(1, variants.running().size());
    }

    @Test
    @DisplayName("A survivor without a free spot does not block the next one in line")
    void skipsASurvivorWithoutASpot(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        Player second = join(env, instance, 200);
        // no floor near the first survivor, floor everywhere near the second
        CreekVariants variants = variants(instance, CONFIG,
                candidate -> candidate.x() > 100 ? Optional.of(candidate) : Optional.empty(), 0L);

        variants.tick(new SurvivorSnapshot(List.of(first, second), List.of(view(first, 0.5D), view(second, 0.3D))), 0L);

        assertEquals(List.of(second.getUuid()), List.copyOf(variants.running().keySet()));
    }

    @Test
    @DisplayName("Stopping removes every variant")
    void stopRemovesEveryVariant(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = join(env, instance, 0);
        CreekVariants variants = variants(instance, CONFIG, Optional::of, 0L);
        variants.tick(new SurvivorSnapshot(List.of(first), List.of(view(first, 0.5D))), 0L);
        Creek variant = variants.running().get(first.getUuid());

        variants.stop();

        assertTrue(variants.running().isEmpty());
        assertTrue(variant.body().entity().isRemoved());
    }
}
