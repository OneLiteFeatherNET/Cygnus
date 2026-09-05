package net.onelitefeather.cygnus.gaze;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the level the service reports while the slender stands in a survivor's view.
 *
 * <p>The service used to draw the tearing itself, as a {@code camera_overlay} texture on the
 * player's head. It now only works out how bad the sight of him is; drawing it is the resource
 * pack's job.</p>
 *
 * @author TheMeinerLP
 * @version 3.0.0
 * @since 2.7.0
 */
class SlenderGazeServiceTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("Seeing the slender reports a level")
    void seeingHimReportsALevel(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(survivor);

        service.tick();

        assertTrue(service.levelOf(survivor) >= 0, "he is right in front of them");
    }

    @Test
    @DisplayName("With him behind them there is nothing to report")
    void behindThemNothingHappens(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, -5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(survivor);

        service.tick();

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor), "the effect is about seeing him");
    }

    @Test
    @DisplayName("Looking away drops the level again")
    void lookingAwayClearsIt(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(survivor);
        service.tick();

        survivor.teleport(new Pos(0, 40, 0, 180, 0));
        service.tick();

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor));
    }

    @Test
    @DisplayName("The nearer he stands, the higher the level")
    void nearerMeansAHigherLevel(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 25));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(survivor);

        service.tick();
        int far = service.levelOf(survivor);

        slender.teleport(new Pos(0, 40, 5));
        service.tick();

        assertTrue(service.levelOf(survivor) > far, "closing in has to make it worse");
    }

    @Test
    @DisplayName("Without a slender nothing happens at all")
    void withoutASlenderNothingHappens(Env env) {
        Player survivor = connect(env, env.createFlatInstance(), new Pos(0, 40, 0, 0, 0));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> null);
        service.track(survivor);

        service.tick();

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor));
    }

    @Test
    @DisplayName("A removed survivor reports nothing")
    void removedSurvivorReportsNone(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(survivor);
        service.tick();

        service.remove(survivor);
        service.tick();

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor));
    }

    @Test
    @DisplayName("Clearing everyone forgets every survivor")
    void cleanUpWipesEveryone(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player second = connect(env, instance, new Pos(4, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.track(first);
        service.track(second);
        service.tick();

        service.cleanUp();

        assertEquals(SlenderGaze.NONE, service.levelOf(first));
        assertEquals(SlenderGaze.NONE, service.levelOf(second));

        service.tick();
        assertEquals(SlenderGaze.NONE, service.levelOf(first), "cleanUp must stop the tracking as well");
    }

    @Test
    @DisplayName("The start of a round takes the survivors on board")
    void gameStartTracksSurvivors(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));

        EventDispatcher.call(new GameStartEvent());
        service.tick();

        assertTrue(service.levelOf(survivor) >= 0);
    }

    @Test
    @DisplayName("A dying survivor is dropped")
    void deathRemovesTheSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.track(survivor);
        service.tick();

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));
        service.tick();

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor));
    }

    @Test
    @DisplayName("The end of a round clears everyone")
    void gameFinishClearsEveryone(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(GazeSink.NONE, () -> slender);
        service.registerListener(env.process().eventHandler(), () -> Set.of(survivor));
        service.track(survivor);
        service.tick();

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertEquals(SlenderGaze.NONE, service.levelOf(survivor));
    }

    @Test
    @DisplayName("The sink hears about a level only when it changes")
    void sinkOnlyHearsAboutChanges(Env env) {
        RecordingSink sink = new RecordingSink();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(sink, () -> slender);
        service.track(survivor);

        service.tick();
        int afterFirst = sink.levels.size();
        service.tick();
        service.tick();

        assertEquals(1, afterFirst, "the first tick moves off NONE, so it reports once");
        assertEquals(afterFirst, sink.levels.size(), "standing still must not repeat the level");
    }

    @Test
    @DisplayName("Losing sight of him reports once more")
    void losingSightReportsAgain(Env env) {
        RecordingSink sink = new RecordingSink();
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0, 0, 0));
        Player slender = connect(env, instance, new Pos(0, 40, 5));
        SlenderGazeService service = new SlenderGazeService(sink, () -> slender);
        service.track(survivor);
        service.tick();

        survivor.teleport(new Pos(0, 40, 0, 180, 0));
        service.tick();

        assertEquals(SlenderGaze.NONE, sink.levels.getLast());
    }

    @Test
    @DisplayName("Tracking and dropping a survivor reaches the sink")
    void trackAndRemoveReachTheSink(Env env) {
        RecordingSink sink = new RecordingSink();
        Player survivor = connect(env, env.createFlatInstance(), new Pos(0, 40, 0, 0, 0));
        SlenderGazeService service = new SlenderGazeService(sink, () -> null);

        service.track(survivor);
        assertEquals(1, sink.attached);

        service.remove(survivor);
        assertEquals(1, sink.detached);

        service.remove(survivor);
        assertEquals(1, sink.detached, "dropping someone twice must not signal twice");
    }

    /** Records what the service hands it, so the tests can look at the traffic. */
    private static final class RecordingSink implements GazeSink {

        private final List<Integer> levels = new ArrayList<>();
        private int attached;
        private int detached;

        @Override
        public void attach(Player survivor) {
            this.attached++;
        }

        @Override
        public void detach(Player survivor) {
            this.detached++;
        }

        @Override
        public void level(Player survivor, int level) {
            this.levels.add(level);
        }
    }

    /**
     * Connects a player at the given position.
     *
     * @param env      the test environment
     * @param instance the instance to connect into
     * @param position where to place them
     * @return the connected player
     */
    private Player connect(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }
}
