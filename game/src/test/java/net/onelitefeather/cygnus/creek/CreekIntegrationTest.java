package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.state.Contexts;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.state.WanderState;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekIntegrationTest extends CygnusPlayerTestBase {

    private static Creek creek(CreekBody body, CreekConfig config, CreekState initial) {
        CreekSight sight = new CreekSight(config.sightRange(), config.sightViewAngle());
        return new Creek(body, sight, (_, _, _) -> 0.0D, Contexts.route(), new SpotFinder(sight, Optional::of),
                _ -> {}, config, new Random(3), initial);
    }

    @Test
    @DisplayName("Wandering, he is seen by the survivors but never by the slender")
    void wanderingIsPublicButNotForTheSlender(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));

        creek(body, CreekConfig.DEFAULT, new WanderState(Long.MAX_VALUE)).tick(List.of(survivor), 0L);

        assertTrue(body.entity().getViewers().contains(survivor));
        assertFalse(body.entity().getViewers().contains(slender));
    }

    @Test
    @DisplayName("A survivor looking at him counts as seeing him")
    void lookingCountsAsSeeing(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        Creek creek = creek(body, CreekConfig.DEFAULT, new WanderState(Long.MAX_VALUE));
        creek.tick(List.of(survivor), 0L);

        assertTrue(creek.views(List.of(survivor)).getFirst().seesCreek());
    }

    @Test
    @DisplayName("Someone he is hidden from does not see him, however they look")
    void hiddenFromNonTargets(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(10, 40, 0, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        Creek creek = creek(body, CreekConfig.DEFAULT, new StalkState(target.getUuid(), Long.MAX_VALUE));
        creek.tick(List.of(target, other), 0L);

        SurvivorView otherView = creek.views(List.of(target, other)).stream()
                .filter(view -> view.id().equals(other.getUuid()))
                .findFirst().orElseThrow();
        assertFalse(otherView.seesCreek());
    }

    @Test
    @DisplayName("When the target leaves mid-hunt he vanishes")
    void targetLeavesMidHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        Player other = env.createConnection().connect(instance, new Pos(40, 40, 40));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(target.getUuid(), Long.MAX_VALUE));
        creek.tick(List.of(target, other), 0L);

        creek.tick(List.of(other), 100L);

        assertInstanceOf(VanishState.class, creek.state());
        assertTrue(body.entity().getViewers().isEmpty());
    }

    @Test
    @DisplayName("With the last survivor and the setting off he is gone for good")
    void dormantWithTheLastSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 30));
        Creek creek = creek(body, withoutLastSurvivor(), new WanderState(Long.MAX_VALUE));

        creek.tick(List.of(survivor), 0L);

        assertInstanceOf(VanishState.class, creek.state());
        assertTrue(((VanishState) creek.state()).isForever());
    }

    private static CreekConfig withoutLastSurvivor() {
        CreekConfig d = CreekConfig.DEFAULT;
        return new CreekConfig(
                d.enabled(), false, d.sightRange(), d.sightViewAngle(),
                d.wanderPauseMillis(), d.wanderSpeed(), d.huntSpeed(), d.stalkThreshold(), d.huntThreshold(),
                d.stalkMinDistance(), d.stalkMaxDistance(), d.stalkMinAngle(), d.stalkMaxAngle(),
                d.stalkRevealMillis(), d.stalkMinSeconds(), d.stalkMaxSeconds(), d.huntMaxSeconds(),
                d.catchDistance(), d.vanishMinSeconds(), d.vanishMaxSeconds(), d.respawnMinDistance(),
                d.personalSpace(), d.stuckMillis(), d.dreadPageWeight(), d.dreadTimeWeight(),
                d.dreadIsolationWeight(), d.isolationRadius(), d.betrayalCatchCount(),
                d.betrayalChance(), d.betrayalGlowSeconds(), d.slownessSeconds());
    }
}
