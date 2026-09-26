package net.onelitefeather.cygnus.creek.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.Contexts;
import net.onelitefeather.cygnus.creek.state.CreekContext;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.DoneState;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.state.PatrolState;
import net.onelitefeather.cygnus.creek.state.RecordingBody;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekDebugTest {

    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();
    private static final Map<UUID, String> NAMES = Map.of(STEVE, "Steve", ALEX, "Alex");

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    @DisplayName("A hunt names its target, the distance, every dread and who sees him")
    void huntLine() {
        List<SurvivorView> views = List.of(
                new SurvivorView(STEVE, new Pos(0, 40, 12.4), 0.62D, true),
                new SurvivorView(ALEX, new Pos(50, 40, 0), 0.18D, false));

        Component line = CreekDebug.line(new HuntState(STEVE, Long.MAX_VALUE), new Pos(0, 40, 0), views, NAMES::get);

        assertEquals("HUNT → Steve · 12.4 m · Steve 0.62 ◉ · Alex 0.18", plain(line));
    }

    @Test
    @DisplayName("Patrolling there is no target, only the dread of everyone")
    void wanderLine() {
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 30), 0.1D, false));

        Component line = CreekDebug.line(new PatrolState(), Pos.ZERO, views, NAMES::get);

        assertEquals("PATROL · Steve 0.10", plain(line));
    }

    @Test
    @DisplayName("Every state has its own label")
    void labels() {
        assertEquals("STALK", CreekDebug.label(new StalkState(STEVE, 0L)));
        assertEquals("VANISH", CreekDebug.label(VanishState.forever()));
        assertEquals("PATROL", CreekDebug.label(new PatrolState()));
        assertEquals("DONE", CreekDebug.label(DoneState.INSTANCE));
    }

    @Test
    @DisplayName("Toggling switches a watcher on and off again")
    void toggle() {
        CreekDebug debug = new CreekDebug();

        assertTrue(debug.toggle(STEVE));
        assertTrue(debug.hasWatchers());
        assertFalse(debug.toggle(STEVE));
        assertFalse(debug.hasWatchers());
    }

    @Test
    @DisplayName("The route description follows the label")
    void routeIsShown() {
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 30), 0.1D, false));

        Component line = CreekDebug.line(new PatrolState(), Pos.ZERO, views, NAMES::get, "Waldweg Nord 3/7 →");

        assertEquals("PATROL · Waldweg Nord 3/7 → · Steve 0.10", plain(line));
    }

    @Test
    @DisplayName("Without a route description the line is unchanged")
    void emptyRouteAddsNothing() {
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 30), 0.1D, false));

        assertEquals("PATROL · Steve 0.10", plain(CreekDebug.line(new PatrolState(), Pos.ZERO, views, NAMES::get, "")));
    }

    private static final Pos HERE = new Pos(0, 40, 0);
    private static final SpotFinder SPOTS = new SpotFinder(new CreekSight(48, 35), Optional::of);

    @Test
    @DisplayName("While staring the patrol names the selected survivor")
    void patrolStareIsShown() {
        PatrolState patrol = new PatrolState();
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 2), 0.1D, false));
        CreekContext ctx = new CreekContext(0L, views, new RecordingBody(HERE), Contexts.route(new Pos(30, 40, 0)),
                SPOTS, _ -> {}, _ -> {}, CreekConfig.DEFAULT, new Random(1));
        patrol.enter(ctx);
        patrol.tick(ctx);

        assertEquals("PATROL → Steve · 2.0 m · Steve 0.10",
                plain(CreekDebug.line(patrol, HERE, views, NAMES::get, "", 0L)));
    }

    @Test
    @DisplayName("The variants show how many run and who they haunt")
    void variantsSegment() {
        List<CreekState> states = List.of(new StalkState(STEVE, 34_000L), new HuntState(ALEX, 12_000L));

        assertEquals(" | variants 2/2 · Steve STALK 34s · Alex HUNT 12s",
                plain(CreekDebug.variants(states, 2, NAMES::get, 0L)));
    }

    @Test
    @DisplayName("Without variants only the count is shown")
    void noVariants() {
        assertEquals(" | variants 0/1", plain(CreekDebug.variants(List.of(), 1, NAMES::get, 0L)));
    }

    @Test
    @DisplayName("During the selection cooldown the patrol shows the seconds left")
    void selectCooldownIsShown() {
        PatrolState patrol = new PatrolState();
        List<SurvivorView> near = List.of(new SurvivorView(STEVE, new Pos(0, 40, 2), 0.1D, false));
        RecordingBody body = new RecordingBody(HERE);
        CreekContext start = new CreekContext(0L, near, body, Contexts.route(new Pos(30, 40, 0)), SPOTS,
                _ -> {}, _ -> {}, CreekConfig.DEFAULT, new Random(1));
        patrol.enter(start);
        patrol.tick(start);
        patrol.tick(new CreekContext(1000L, near, body, Contexts.route(new Pos(30, 40, 0)), SPOTS,
                _ -> {}, _ -> {}, CreekConfig.DEFAULT, new Random(1)));

        assertEquals("PATROL · select 7s · Steve 0.10",
                plain(CreekDebug.line(patrol, HERE, near, NAMES::get, "", 4000L)));
    }
}
