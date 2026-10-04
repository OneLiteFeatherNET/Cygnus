package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.PatrolHelper;
import net.onelitefeather.cygnus.creek.consequence.StalkSounds;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.state.Contexts;
import net.onelitefeather.cygnus.creek.state.DoneState;
import net.onelitefeather.cygnus.creek.state.CreekContext;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.HuntCooldowns;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.state.PatrolState;
import net.onelitefeather.cygnus.creek.tab.HuntedTabWitness;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekIntegrationTest extends CygnusPlayerTestBase {

    private static Creek creek(CreekBody body, CreekConfig config, CreekState initial) {
        return creek(body, config, initial, CreekWitness.NONE);
    }

    private static Creek creek(CreekBody body, CreekConfig config, CreekState initial, CreekWitness witness) {
        CreekSight sight = new CreekSight(config.sightRange(), config.sightViewAngle());
        // nextBoolean() is true for -1, so a selection always ends in the stun.
        PatrolHelper patrol = new PatrolHelper(() -> -1L);
        CreekRound round = new CreekRound(sight, new SpotFinder(sight, Optional::of), _ -> {}, witness, patrol,
                new StalkSounds(new Random(3)), config, new Random(3), HuntCooldowns.none());
        return new Creek(body, Contexts.route(), round, initial);
    }

    /** Writes down what the creek reports. */
    private static final class RecordingWitness implements CreekWitness {
        private final List<UUID> sightings = new ArrayList<>();
        private final List<UUID> catches = new ArrayList<>();
        private final List<UUID> stalks = new ArrayList<>();
        private final List<UUID> selections = new ArrayList<>();
        private final List<UUID> hunts = new ArrayList<>();
        private final List<UUID> huntEnds = new ArrayList<>();

        @Override
        public void hunted(UUID survivor) {
            this.hunts.add(survivor);
        }

        @Override
        public void huntEnded(UUID survivor) {
            this.huntEnds.add(survivor);
        }

        @Override
        public void sighted(UUID survivor) {
            this.sightings.add(survivor);
        }

        @Override
        public void caught(UUID survivor) {
            this.catches.add(survivor);
        }

        @Override
        public void stalked(UUID survivor) {
            this.stalks.add(survivor);
        }

        @Override
        public void selected(UUID survivor) {
            this.selections.add(survivor);
        }
    }

    /** A snapshot of the players, none of them scared. */
    private static SurvivorSnapshot survivors(Player... players) {
        return SurvivorSnapshot.take(List.of(players), _ -> 0.0D);
    }

    @Test
    @DisplayName("Wandering, he is seen by the survivors but never by the slender")
    void wanderingIsPublicButNotForTheSlender(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));

        creek(body, CreekConfig.DEFAULT, new PatrolState()).tick(survivors(survivor), 0L);

        assertTrue(body.entity().getViewers().contains(survivor));
        assertFalse(body.entity().getViewers().contains(slender));
    }

    @Test
    @DisplayName("A survivor looking at him counts as seeing him")
    void lookingCountsAsSeeing(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState());
        creek.tick(survivors(survivor), 0L);

        assertTrue(creek.views(survivors(survivor)).getFirst().seesCreek());
    }

    @Test
    @DisplayName("Close by and looking away, a survivor still has him in sight")
    void closeByIsInSight(Env env) {
        Instance instance = env.createFlatInstance();
        // Looking towards negative Z, away from him.
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 3));
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState());
        creek.tick(survivors(survivor), 0L);

        SurvivorView view = creek.views(survivors(survivor)).getFirst();
        assertFalse(view.seesCreek());
        assertTrue(view.inSight());
    }

    @Test
    @DisplayName("Far away and looking away, the line of sight is not checked")
    void farAwaySkipsTheRay(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, Creek.SIGHT_CHECK_DISTANCE + 2));
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState());
        creek.tick(survivors(survivor), 0L);

        assertFalse(creek.views(survivors(survivor)).getFirst().inSight());
    }

    @Test
    @DisplayName("Someone he is hidden from does not see him, however they look")
    void hiddenFromNonTargets(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(10, 40, 0, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        Creek creek = creek(body, CreekConfig.DEFAULT, new StalkState(target.getUuid(), Long.MAX_VALUE));
        creek.tick(survivors(target, other), 0L);

        SurvivorView otherView = creek.views(survivors(target, other)).stream()
                .filter(view -> view.id().equals(other.getUuid()))
                .findFirst().orElseThrow();
        assertFalse(otherView.seesCreek());
    }

    @Test
    @DisplayName("When the target leaves mid-hunt he is done")
    void targetLeavesMidHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        Player other = env.createConnection().connect(instance, new Pos(40, 40, 40));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(target.getUuid(), Long.MAX_VALUE));
        creek.tick(survivors(target, other), 0L);

        creek.tick(survivors(other), 100L);

        assertInstanceOf(DoneState.class, creek.state());
        assertTrue(body.entity().getViewers().isEmpty());
    }

    @Test
    @DisplayName("Vanishing for good hides him from everyone for the rest of the round")
    void vanishForGood(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 30));
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState());
        creek.tick(survivors(survivor), 0L);

        creek.vanishForGood(survivors(survivor), 100L);
        CreekState vanished = creek.state();
        creek.vanishForGood(survivors(survivor), 200L);

        assertTrue(((VanishState) vanished).isForever());
        assertSame(vanished, creek.state(), "calling it again changes nothing");
        assertFalse(body.isVisibleTo(survivor.getUuid()));
    }

    @Test
    @DisplayName("A state selecting a survivor hands them to the patrol helper")
    void selectionReachesTheConsequence(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        CreekState selecting = new CreekState() {
            @Override
            public void enter(CreekContext ctx) {
            }

            @Override
            public CreekState tick(CreekContext ctx) {
                ctx.actions().selected(survivor.getUuid());
                return this;
            }
        };

        RecordingWitness witness = new RecordingWitness();
        creek(body, CreekConfig.DEFAULT, selecting, witness).tick(survivors(survivor), 0L);

        assertTrue(survivor.hasEffect(PotionEffect.SLOWNESS));
        assertEquals(List.of(survivor.getUuid()), witness.selections);
    }

    @Test
    @DisplayName("Vanishing blinds the survivors nearby, but not the ones the creek leaves alone")
    void vanishSparesTheIgnored(Env env) {
        Instance instance = env.createFlatInstance();
        Player haunted = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player other = env.createConnection().connect(instance, new Pos(3, 40, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 2));
        CreekState vanishing = new CreekState() {
            @Override
            public void enter(CreekContext ctx) {
            }

            @Override
            public CreekState tick(CreekContext ctx) {
                ctx.actions().vanished(ctx.body().position());
                return this;
            }
        };

        creek(body, CreekConfig.DEFAULT, vanishing).tick(survivors(haunted, other), Set.of(haunted.getUuid()), 0L);

        assertTrue(other.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(haunted.hasEffect(PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("A haunted survivor does not see the patrolling creek, everyone else does")
    void patrolIsHiddenFromTheHaunted(Env env) {
        Instance instance = env.createFlatInstance();
        Player haunted = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(5, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));

        creek(body, CreekConfig.DEFAULT, new PatrolState())
                .tick(survivors(haunted, other), Set.of(haunted.getUuid()), 0L);

        assertFalse(body.isVisibleTo(haunted.getUuid()));
        assertTrue(body.isVisibleTo(other.getUuid()));
    }

    @Test
    @DisplayName("The patrolling creek never picks out a haunted survivor")
    void patrolLeavesTheHauntedAlone(Env env) {
        Instance instance = env.createFlatInstance();
        Player haunted = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, 60, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 2));
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState());

        creek.tick(survivors(haunted, other), Set.of(haunted.getUuid()), 0L);
        creek.tick(survivors(haunted, other), Set.of(haunted.getUuid()), 1000L);

        assertFalse(haunted.hasEffect(PotionEffect.SLOWNESS));
        assertEquals(2, creek.lastViews().size(), "the snapshots still cover every survivor");
    }

    @Test
    @DisplayName("Spotting him is reported once, not for every step he stays in view")
    void sightingIsReportedOnce(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);

        // The first step only enters the hunt, which shows him to the target; he is seen from the next one on.
        creek.tick(survivors(survivor), 0L);
        creek.tick(survivors(survivor), 100L);
        creek.tick(survivors(survivor), 200L);

        assertEquals(List.of(survivor.getUuid()), witness.sightings);
    }

    @Test
    @DisplayName("Looking away and back again is a new sighting")
    void lookingBackIsANewSighting(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);
        creek.tick(survivors(survivor), 0L);
        creek.tick(survivors(survivor), 100L);

        survivor.teleport(new Pos(0, 40, 0, 180, 0)).join();
        creek.tick(survivors(survivor), 200L);
        survivor.teleport(new Pos(0, 40, 0, 0, 0)).join();
        creek.tick(survivors(survivor), 300L);

        assertEquals(List.of(survivor.getUuid(), survivor.getUuid()), witness.sightings);
    }

    @Test
    @DisplayName("Survivors he leaves alone are not reported as seeing him")
    void ignoredSurvivorsAreNotReported(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new PatrolState(), witness);

        creek.tick(survivors(survivor), Set.of(survivor.getUuid()), 0L);
        creek.tick(survivors(survivor), Set.of(survivor.getUuid()), 100L);

        assertTrue(witness.sightings.isEmpty());
    }

    @Test
    @DisplayName("A hunt is reported once when it starts, not for every step")
    void huntIsReportedOnce(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);

        creek.tick(survivors(survivor), 0L);
        creek.tick(survivors(survivor), 100L);

        assertEquals(List.of(survivor.getUuid()), witness.hunts);
        assertTrue(witness.huntEnds.isEmpty());
    }

    @Test
    @DisplayName("A stalk is not a hunt")
    void stalkIsNotAHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 25));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new StalkState(target.getUuid(), Long.MAX_VALUE), witness);

        creek.tick(survivors(target), 0L);

        assertTrue(witness.hunts.isEmpty());
    }

    @Test
    @DisplayName("A catch ends the hunt")
    void catchEndsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 1));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);

        creek.tick(survivors(survivor), 0L);

        assertEquals(List.of(survivor.getUuid()), witness.hunts);
        assertEquals(List.of(survivor.getUuid()), witness.huntEnds);
    }

    @Test
    @DisplayName("A hunt that runs out of time ends")
    void timeoutEndsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), 500L), witness);
        creek.tick(survivors(survivor), 0L);

        creek.tick(survivors(survivor), 600L);

        assertEquals(List.of(survivor.getUuid()), witness.huntEnds);
    }

    @Test
    @DisplayName("A survivor who disappears from the round ends the hunt")
    void goneSurvivorEndsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(5, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 10));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(target.getUuid(), Long.MAX_VALUE), witness);
        creek.tick(survivors(target, other), 0L);

        creek.tick(survivors(other), 100L);

        assertEquals(List.of(target.getUuid()), witness.huntEnds);
    }

    @Test
    @DisplayName("Removing a creek in the middle of a hunt ends it")
    void removingEndsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);
        creek.tick(survivors(survivor), 0L);

        creek.remove();
        creek.remove();

        assertEquals(List.of(survivor.getUuid()), witness.huntEnds);
    }

    @Test
    @DisplayName("Sending a hunting creek away for good ends the hunt")
    void vanishingEndsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 0, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);
        creek.tick(survivors(survivor), 0L);

        creek.vanishForGood(survivors(survivor), 100L);

        assertEquals(List.of(survivor.getUuid()), witness.huntEnds);
    }

    @Test
    @DisplayName("Through the creek, the tab name is marked during the hunt and back to normal after the catch")
    void tabNameFollowsTheHunt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        Component before = Component.text(survivor.getUsername());
        survivor.setDisplayName(before);
        CreakingBody far = CreakingBody.spawn(instance, new Pos(0, 40, 20));
        HuntedTabWitness witness = new HuntedTabWitness(CreekWitness.NONE,
                id -> id.equals(survivor.getUuid()) ? survivor : null);
        Creek hunting = creek(far, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);

        hunting.tick(survivors(survivor), 0L);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(survivor.getDisplayName())
                .contains(HuntedTabWitness.MARKER));

        hunting.remove();
        assertEquals(before, survivor.getDisplayName());
    }

    @Test
    @DisplayName("A catch is reported")
    void catchIsReported(Env env) {
        Instance instance = env.createFlatInstance();
        // Facing away, so the creek is not frozen by being watched.
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 1));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new HuntState(survivor.getUuid(), Long.MAX_VALUE), witness);

        creek.tick(survivors(survivor), 0L);

        assertEquals(List.of(survivor.getUuid()), witness.catches);
    }

    @Test
    @DisplayName("Every step of a stalk is reported for its target")
    void stalkIsReported(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 25));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new StalkState(target.getUuid(), Long.MAX_VALUE), witness);

        creek.tick(survivors(target), 0L);
        creek.tick(survivors(target), 100L);

        assertEquals(List.of(target.getUuid(), target.getUuid()), witness.stalks);
    }

    @Test
    @DisplayName("A stalk that ends in this step is not reported")
    void endedStalkIsNotReported(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0, 180, 0));
        CreakingBody body = CreakingBody.spawn(instance, new Pos(0, 40, 25));
        RecordingWitness witness = new RecordingWitness();
        Creek creek = creek(body, CreekConfig.DEFAULT, new StalkState(target.getUuid(), 0L), witness);

        creek.tick(survivors(target), 0L);

        assertTrue(witness.stalks.isEmpty());
    }
}
