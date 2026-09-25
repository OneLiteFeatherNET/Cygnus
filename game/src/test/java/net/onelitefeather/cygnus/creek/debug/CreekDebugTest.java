package net.onelitefeather.cygnus.creek.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.state.HuntState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.state.WanderState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    @DisplayName("Wandering there is no target, only the dread of everyone")
    void wanderLine() {
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 30), 0.1D, false));

        Component line = CreekDebug.line(new WanderState(0L), Pos.ZERO, views, NAMES::get);

        assertEquals("WANDER · Steve 0.10", plain(line));
    }

    @Test
    @DisplayName("Every state has its own label")
    void labels() {
        assertEquals("STALK", CreekDebug.label(new StalkState(STEVE, 0L)));
        assertEquals("VANISH", CreekDebug.label(VanishState.forever()));
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

        Component line = CreekDebug.line(new WanderState(0L), Pos.ZERO, views, NAMES::get, "Waldweg Nord 3/7 →");

        assertEquals("WANDER · Waldweg Nord 3/7 → · Steve 0.10", plain(line));
    }

    @Test
    @DisplayName("Without a route description the line is unchanged")
    void emptyRouteAddsNothing() {
        List<SurvivorView> views = List.of(new SurvivorView(STEVE, new Pos(0, 40, 30), 0.1D, false));

        assertEquals("WANDER · Steve 0.10", plain(CreekDebug.line(new WanderState(0L), Pos.ZERO, views, NAMES::get, "")));
    }
}
