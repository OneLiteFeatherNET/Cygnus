package net.onelitefeather.cygnus.glitch;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.gaze.GazeSink;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the glitch the slender's own screen picks up as his pages are taken.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.14.0
 */
class PageGlitchServiceTest extends CygnusPlayerTestBase {

    /** How many pages a round needs in every test that does not say otherwise. */
    private static final int PAGES = 4;

    /** How long a find's pulse holds, in seconds. */
    private static final int PULSE_SECONDS = 2;

    /** The strongest level there is. */
    private static final int WORST = SlenderGaze.LEVELS - 1;

    @Test
    @DisplayName("The slender's screen is clean until the first page goes")
    void beforeTheFirstPageTheScreenIsClean(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new GameStartEvent());
        service.tick();

        assertEquals(List.of(slender), sink.attached, "he has to be attached, ready for a level");
        assertEquals(List.of(), sink.levels, "an untouched round is not worth a single frame of tearing");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The last page leaves his screen at its worst")
    void theLastPageIsTheWorstItGets(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        tick(service, PULSE_SECONDS);

        assertEquals(WORST, sink.lastLevel(), "every page gone has to read as the worst level there is");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A find pulses one level above where the round stands")
    void aFindPulsesAboveTheBaseline(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));

        int pulse = sink.lastLevel();
        tick(service, PULSE_SECONDS);
        int baseline = sink.lastLevel();

        assertTrue(pulse > baseline, "the moment a page goes has to hit harder than the state it leaves behind");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The pulse holds for the configured seconds")
    void thePulseHoldsForItsSeconds(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));
        int pulse = sink.lastLevel();

        tick(service, PULSE_SECONDS - 1);
        assertEquals(pulse, sink.lastLevel(), "the pulse must not fall off early");
        tick(service, 1);

        assertTrue(sink.lastLevel() < pulse, "once its seconds are up the pulse has to fall back");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("An unchanged level is never sent twice")
    void anUnchangedLevelIsNeverRepeated(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));

        tick(service, PULSE_SECONDS + 5);

        assertEquals(sink.levels.stream().distinct().toList(), sink.levels,
                "a sink is only ever told about a change, repeating one costs the client for nothing");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A new slender takes the glitch over from the old one")
    void aNewSlenderTakesItOver(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance, new Pos(4, 40, 0));
        AtomicReference<Player> slender = new AtomicReference<>(first);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = new PageGlitchService(config(true), sink, slender::get);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(first, PAGES, PAGES));

        slender.set(second);
        service.tick();

        assertEquals(List.of(first), sink.detached, "the old slender must not keep a screen full of tearing");
        assertEquals(List.of(first, second), sink.attached);
        assertSame(second, sink.lastPlayer(), "the round's state belongs to whoever is the slender now");
        assertEquals(WORST, sink.lastLevel(), "the pages stay found across a hand-over");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The end of the round takes the glitch off his screen")
    void theEndOfTheRoundClearsHisScreen(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));

        assertEquals(List.of(slender), sink.detached);
        sink.levels.clear();
        EventDispatcher.call(new GameStartEvent());
        service.tick();
        assertEquals(List.of(), sink.levels, "a fresh round starts with the pages unfound again");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A turned off feature never touches his screen")
    void aTurnedOffFeatureStaysAway(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(false), sink, slender);
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        tick(service, PULSE_SECONDS + 1);

        assertEquals(List.of(), sink.attached);
        assertEquals(List.of(), sink.levels);

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Without a slender there is no screen to tear")
    void withoutASlenderNothingHappens(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = new PageGlitchService(config(true), sink, () -> null);
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new GameStartEvent());
        tick(service, PULSE_SECONDS + 1);

        assertEquals(List.of(), sink.attached);
        assertEquals(List.of(), sink.levels);

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The slender keeps his sight: the veil comes without the world tint")
    void theWorldIsNeverDarkenedForHim(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        RecordingSink sink = new RecordingSink();
        PageGlitchService service = service(config(true), sink, slender);
        service.registerListener(env.process().eventHandler());

        EventDispatcher.call(new GameStartEvent());

        assertEquals(List.of(false), sink.worldTints,
                "darkening the world would blind the hunter, which is the one thing he must not lose");

        env.destroyInstance(instance, true);
    }

    /**
     * Runs the service's second by hand, so a test does not have to wait for the scheduler.
     *
     * @param service the service under test
     * @param seconds how many seconds to run
     */
    private static void tick(PageGlitchService service, int seconds) {
        for (int i = 0; i < seconds; i++) {
            service.tick();
        }
    }

    /**
     * Builds a service for a slender who never changes.
     *
     * @param config  the configuration to run with
     * @param sink    where the levels go
     * @param slender the slender
     * @return the service under test
     */
    private static PageGlitchService service(GameConfig config, GazeSink sink, Player slender) {
        return new PageGlitchService(config, sink, () -> slender);
    }

    /**
     * Builds a configuration that only says something about the glitch.
     *
     * @param enabled whether the glitch is on
     * @return the configuration
     */
    private static GameConfig config(boolean enabled) {
        return GameConfig.builder()
                .pageGlitchEnabled(enabled)
                .pageGlitchPulseSeconds(PULSE_SECONDS)
                .build();
    }

    /**
     * A sink that writes down what it was told instead of drawing anything.
     */
    private static final class RecordingSink implements GazeSink {

        private final List<Player> attached = new ArrayList<>();
        private final List<Boolean> worldTints = new ArrayList<>();
        private final List<Player> detached = new ArrayList<>();
        private final List<Integer> levels = new ArrayList<>();
        private final List<Player> players = new ArrayList<>();

        @Override
        public void attach(Player player) {
            this.attach(player, true);
        }

        @Override
        public void attach(Player player, boolean worldTint) {
            this.attached.add(player);
            this.worldTints.add(worldTint);
        }

        @Override
        public void detach(Player player) {
            this.detached.add(player);
        }

        @Override
        public void level(Player player, int level) {
            this.players.add(player);
            this.levels.add(level);
        }

        /**
         * @return the level reported last, or {@link SlenderGaze#NONE} if there was none
         */
        private int lastLevel() {
            return this.levels.isEmpty() ? SlenderGaze.NONE : this.levels.getLast();
        }

        /**
         * @return the player the last level was reported for
         */
        private Player lastPlayer() {
            return this.players.getLast();
        }
    }
}
