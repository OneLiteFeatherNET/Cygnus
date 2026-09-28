package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekContextTest {

    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();

    private static CreekContext context() {
        return Contexts.context(0L, new RecordingBody(Pos.ZERO), Contexts.route(), new ArrayList<>(),
                new SurvivorView(FIRST, new Pos(0, 40, 0), 0.2D, false),
                new SurvivorView(SECOND, new Pos(30, 40, 0), 0.5D, true));
    }

    @Test
    @DisplayName("A survivor is found by id")
    void findsSurvivor() {
        assertEquals(SECOND, context().survivor(SECOND).orElseThrow().id());
        assertTrue(context().survivor(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("The ids of every survivor")
    void survivorIds() {
        assertEquals(Set.of(FIRST, SECOND), context().survivorIds());
    }

    @Test
    @DisplayName("The highest dread wins, and none means zero")
    void highestDread() {
        assertEquals(0.5D, context().highestDread());
        assertEquals(0.0D, Contexts.context(0L, new RecordingBody(Pos.ZERO), Contexts.route(), new ArrayList<>()).highestDread());
    }

    @Test
    @DisplayName("Far from all means far from every survivor")
    void farFromAll() {
        assertTrue(context().farFromAll(new Pos(15, 40, 20), 15));
        assertFalse(context().farFromAll(new Pos(5, 40, 0), 15));
    }

    @Test
    @DisplayName("Observer eyes sit above the feet")
    void observerEyes() {
        assertEquals(List.of(new Pos(0, 41.62, 0), new Pos(30, 41.62, 0)), context().observerEyes());
    }
}
