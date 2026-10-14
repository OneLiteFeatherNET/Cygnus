package net.onelitefeather.cygnus.glitch;

import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.stamina.StaminaBar;
import net.onelitefeather.cygnus.gaze.GazeSink;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the glitch the slender's own screen picks up as his pages are taken.
 *
 * @author TheMeinerLP
 * @version 1.0.3
 * @since 2.15.0
 */
class PageGlitchServiceTest extends CygnusPlayerTestBase {

    /** How many pages a round needs in every test that does not say otherwise. */
    private static final int PAGES = 4;

    /** How long a find's pulse holds, in seconds. */
    private static final int PULSE_SECONDS = 2;

    /** The smallest round there is, which is the one the curve has to feel right on. */
    private static final int MIN_ROUND_PAGES = 8;

    /** The strongest level there is. */
    private static final int WORST = SlenderGaze.LEVELS - 1;

    private final List<EventNode<Event>> nodes = new ArrayList<>();
    private final RecordingSink sink = new RecordingSink();
    private Env env;
    private Instance instance;
    private Player slender;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.slender = env.createPlayer(this.instance);
    }

    @AfterEach
    void tearDown() {
        this.nodes.forEach(this.env.process().eventHandler()::removeChild);
        this.env.destroyInstance(this.instance, true);
    }

    @Test
    @DisplayName("Six of eight pages hold the screen at the first level once the pulse is gone")
    void sixOfEightPagesHoldTheFirstLevel() {
        PageGlitchService service = start(config(true));

        EventDispatcher.call(new PageFoundEvent(slender, 6, MIN_ROUND_PAGES));
        tick(service, PULSE_SECONDS);

        assertEquals(1, sink.lastLevel(), "the glitch has to start slowly, three quarters in is still only the first level");
    }

    @Test
    @DisplayName("A find at six of eight pages pulses to the second level, not the worst")
    void aFindAtSixOfEightDoesNotHitTheWorst() {
        start(config(true));

        EventDispatcher.call(new PageFoundEvent(slender, 6, MIN_ROUND_PAGES));

        assertEquals(2, sink.lastLevel(), "the worst level is for the end of the round, not for the sixth page");
    }

    @Test
    @DisplayName("The last page of the smallest round still reaches the worst level")
    void theLastOfEightPagesIsTheWorst() {
        PageGlitchService service = start(config(true));

        EventDispatcher.call(new PageFoundEvent(slender, MIN_ROUND_PAGES, MIN_ROUND_PAGES));
        tick(service, PULSE_SECONDS);

        assertEquals(WORST, sink.lastLevel(), "the curve may start slowly but it has to arrive");
    }

    @Test
    @DisplayName("With the default cap every page count settles at the weakest level")
    void theDefaultCapHoldsEveryBaselineAtTheWeakestLevel() {
        PageGlitchService service = start(cappedConfig());

        for (int found = 1; found <= MIN_ROUND_PAGES; found++) {
            EventDispatcher.call(new PageFoundEvent(slender, found, MIN_ROUND_PAGES));
            tick(service, PULSE_SECONDS);
            assertEquals(0, sink.lastLevel(), "baseline after " + found + " of " + MIN_ROUND_PAGES + " pages");
        }
    }

    @Test
    @DisplayName("With the default cap a find's pulse never rises above the weakest level")
    void theDefaultCapHoldsEveryPulseAtTheWeakestLevel() {
        PageGlitchService service = start(cappedConfig());

        for (int found = 1; found <= MIN_ROUND_PAGES; found++) {
            EventDispatcher.call(new PageFoundEvent(slender, found, MIN_ROUND_PAGES));
            assertEquals(0, sink.lastLevel(), "pulse at " + found + " of " + MIN_ROUND_PAGES + " pages");
            tick(service, PULSE_SECONDS);
        }
    }

    @Test
    @DisplayName("With the default cap the screen is still clean before the first page")
    void theDefaultCapKeepsTheScreenCleanBeforeTheFirstPage() {
        PageGlitchService service = start(cappedConfig());
        service.tick();

        assertEquals(List.of(), sink.levels, "the cap lowers levels, it does not invent one before a page goes");
    }

    @Test
    @DisplayName("A cap of one limits the last page to the second level")
    void aCapOfOneLimitsTheLastPage() {
        start(new GameConfig.PageGlitch(true, PULSE_SECONDS, 1));

        EventDispatcher.call(new PageFoundEvent(slender, MIN_ROUND_PAGES, MIN_ROUND_PAGES));

        assertEquals(1, sink.lastLevel(), "neither the baseline nor the pulse may pass the cap");
    }

    @Test
    @DisplayName("While he is visible his screen stays clean, however many pages are gone")
    void aVisibleSlenderGetsNoGlitch() {
        AtomicBoolean invisible = new AtomicBoolean(false);
        PageGlitchService service = start(new PageGlitchService(config(true), sink, () -> slender, _ -> invisible.get(), TickSections.NONE));

        EventDispatcher.call(new PageFoundEvent(slender, 2, MIN_ROUND_PAGES));
        tick(service, PULSE_SECONDS);
        EventDispatcher.call(new PageFoundEvent(slender, MIN_ROUND_PAGES, MIN_ROUND_PAGES));

        assertEquals(List.of(), sink.levels, "a hunting slender must not lose sight to his own glitch");
    }

    @Test
    @DisplayName("Turning invisible shows the level the round has reached, turning visible clears it at once")
    void theToggleMovesTheGlitchImmediately() {
        AtomicBoolean invisible = new AtomicBoolean(false);
        PageGlitchService service = start(new PageGlitchService(config(true), sink, () -> slender, _ -> invisible.get(), TickSections.NONE));
        EventDispatcher.call(new PageFoundEvent(slender, MIN_ROUND_PAGES, MIN_ROUND_PAGES));
        tick(service, PULSE_SECONDS);

        invisible.set(true);
        EventDispatcher.call(new StaminaStateChangeEvent(slender, StaminaBar.State.REGENERATING));
        assertEquals(WORST, sink.lastLevel(), "the progress was kept running while he was visible");

        invisible.set(false);
        EventDispatcher.call(new StaminaStateChangeEvent(slender, StaminaBar.State.DRAINING));
        assertEquals(SlenderGaze.NONE, sink.lastLevel(), "no tick may pass with a glitch up while he hunts");
    }

    @Test
    @DisplayName("A toggle after the round has ended does not bring the carrier back")
    void aToggleAfterTheRoundStaysAway() {
        PageGlitchService service = start(new PageGlitchService(config(true), sink, () -> slender, _ -> true, TickSections.NONE));
        EventDispatcher.call(new PageFoundEvent(slender, 1, MIN_ROUND_PAGES));
        service.stop();
        sink.attached.clear();
        sink.levels.clear();

        EventDispatcher.call(new StaminaStateChangeEvent(slender, StaminaBar.State.REGENERATING));

        assertEquals(List.of(), sink.attached, "a finished round must not attach the carrier again");
        assertEquals(List.of(), sink.levels, "a finished round has nothing to draw");
    }

    @Test
    @DisplayName("A pulse keeps counting down while he is visible")
    void thePulseRunsOnWhileHeIsVisible() {
        AtomicBoolean invisible = new AtomicBoolean(false);
        PageGlitchService service = start(new PageGlitchService(config(true), sink, () -> slender, _ -> invisible.get(), TickSections.NONE));
        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));
        tick(service, PULSE_SECONDS);

        invisible.set(true);
        EventDispatcher.call(new StaminaStateChangeEvent(slender, StaminaBar.State.REGENERATING));

        assertEquals(0, sink.lastLevel(), "the pulse ran out while he was visible, only the baseline is left");
    }

    @Test
    @DisplayName("The baseline eases in over the smallest round")
    void theBaselineEasesIn() {
        int[] expected = {0, 0, 0, 0, 1, 1, 2, 3};
        for (int found = 1; found <= MIN_ROUND_PAGES; found++) {
            assertEquals(expected[found - 1], PageGlitchService.baselineOf((float) found / MIN_ROUND_PAGES),
                    "baseline after " + found + " of " + MIN_ROUND_PAGES + " pages");
        }
    }

    @Test
    @DisplayName("The slender's screen is clean until the first page goes")
    void beforeTheFirstPageTheScreenIsClean() {
        PageGlitchService service = start(config(true));
        service.tick();

        assertEquals(List.of(slender), sink.attached, "he has to be attached, ready for a level");
        assertEquals(List.of(), sink.levels, "an untouched round is not worth a single frame of tearing");
    }

    @Test
    @DisplayName("The last page leaves his screen at its worst")
    void theLastPageIsTheWorstItGets() {
        PageGlitchService service = start(config(true));

        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        tick(service, PULSE_SECONDS);

        assertEquals(WORST, sink.lastLevel(), "every page gone has to read as the worst level there is");
    }

    @Test
    @DisplayName("A find pulses one level above where the round stands")
    void aFindPulsesAboveTheBaseline() {
        PageGlitchService service = start(config(true));

        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));

        int pulse = sink.lastLevel();
        tick(service, PULSE_SECONDS);
        int baseline = sink.lastLevel();

        assertTrue(pulse > baseline, "the moment a page goes has to hit harder than the state it leaves behind");
    }

    @Test
    @DisplayName("The pulse holds for the configured seconds")
    void thePulseHoldsForItsSeconds() {
        PageGlitchService service = start(config(true));
        EventDispatcher.call(new PageFoundEvent(slender, 1, PAGES));
        int pulse = sink.lastLevel();

        tick(service, PULSE_SECONDS - 1);
        assertEquals(pulse, sink.lastLevel(), "the pulse must not fall off early");
        tick(service, 1);

        assertTrue(sink.lastLevel() < pulse, "once its seconds are up the pulse has to fall back");
    }

    @Test
    @DisplayName("An unchanged level is never sent twice")
    void anUnchangedLevelIsNeverRepeated() {
        PageGlitchService service = start(config(true));
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));

        tick(service, PULSE_SECONDS + 5);

        assertEquals(sink.levels.stream().distinct().toList(), sink.levels,
                "a sink is only ever told about a change, repeating one costs the client for nothing");
    }

    @Test
    @DisplayName("A new slender takes the glitch over from the old one")
    void aNewSlenderTakesItOver(@NotNull Env env) {
        Player first = this.slender;
        Player second = env.createPlayer(this.instance, new Pos(4, 40, 0));
        AtomicReference<Player> current = new AtomicReference<>(first);
        PageGlitchService service = start(new PageGlitchService(config(true), sink, current::get));
        EventDispatcher.call(new PageFoundEvent(first, PAGES, PAGES));

        current.set(second);
        service.tick();

        assertEquals(List.of(first), sink.detached, "the old slender must not keep a screen full of tearing");
        assertEquals(List.of(first, second), sink.attached);
        assertSame(second, sink.lastPlayer(), "the round's state belongs to whoever is the slender now");
        assertEquals(WORST, sink.lastLevel(), "the pages stay found across a hand-over");
    }

    @Test
    @DisplayName("The end of the round takes the glitch off his screen")
    void theEndOfTheRoundClearsHisScreen() {
        PageGlitchService service = start(config(true));
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));

        assertEquals(List.of(slender), sink.detached);
        sink.levels.clear();
        EventDispatcher.call(new GameStartEvent());
        service.tick();
        assertEquals(List.of(), sink.levels, "a fresh round starts with the pages unfound again");
    }

    @Test
    @DisplayName("A turned off feature never touches his screen")
    void aTurnedOffFeatureStaysAway() {
        PageGlitchService service = start(config(false));
        EventDispatcher.call(new PageFoundEvent(slender, PAGES, PAGES));
        tick(service, PULSE_SECONDS + 1);

        assertEquals(List.of(), sink.attached);
        assertEquals(List.of(), sink.levels);
    }

    @Test
    @DisplayName("Without a slender there is no screen to tear")
    void withoutASlenderNothingHappens() {
        PageGlitchService service = start(new PageGlitchService(config(true), sink, () -> null));
        tick(service, PULSE_SECONDS + 1);

        assertEquals(List.of(), sink.attached);
        assertEquals(List.of(), sink.levels);
    }

    @Test
    @DisplayName("The slender gets the glitch alone: no colour shift")
    void hisGlitchComesWithoutColourShift() {
        start(config(true));

        assertEquals(List.of(false), sink.colourShifts,
                "the purple belongs to the survivors, and darkening the world would blind the hunter");
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
     * Builds a service for the slender who never changes, registers it and starts the round.
     *
     * @param config the configuration to run with
     * @return the service under test
     */
    private PageGlitchService start(GameConfig.PageGlitch config) {
        return this.start(new PageGlitchService(config, this.sink, () -> this.slender));
    }

    /**
     * Registers a service and starts the round.
     *
     * @param service the service under test
     * @return the same service
     */
    private PageGlitchService start(PageGlitchService service) {
        this.nodes.add(service.node());
        this.env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        return service;
    }

    /**
     * Builds a configuration that only says something about the glitch.
     *
     * @param enabled whether the glitch is on
     * @return the page glitch configuration
     */
    private static GameConfig.PageGlitch config(boolean enabled) {
        return new GameConfig.PageGlitch(enabled, PULSE_SECONDS, WORST);
    }

    /**
     * Builds an enabled configuration with the shipped cap, the weakest level only.
     *
     * @return the page glitch configuration
     */
    private static GameConfig.PageGlitch cappedConfig() {
        return new GameConfig.PageGlitch(true, PULSE_SECONDS, GameConfig.PageGlitch.DEFAULT_MAX_LEVEL);
    }

    /**
     * A sink that writes down what it was told instead of drawing anything.
     */
    private static final class RecordingSink implements GazeSink {

        private final List<Player> attached = new ArrayList<>();
        private final List<Boolean> colourShifts = new ArrayList<>();
        private final List<Player> detached = new ArrayList<>();
        private final List<Integer> levels = new ArrayList<>();
        private final List<Player> players = new ArrayList<>();

        @Override
        public void attach(Player player) {
            this.attach(player, true);
        }

        @Override
        public void attach(Player player, boolean colourShift) {
            this.attached.add(player);
            this.colourShifts.add(colourShift);
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
