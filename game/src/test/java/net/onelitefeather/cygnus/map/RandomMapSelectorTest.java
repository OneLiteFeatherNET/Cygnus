package net.onelitefeather.cygnus.map;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RandomMapSelectorTest {

    @Test
    void reachesEveryMapOverManyDraws(@TempDir Path root) throws IOException {
        List<MapCandidate> candidates = List.of(candidate(root, "a"), candidate(root, "b"), candidate(root, "c"));
        RandomMapSelector selector = new RandomMapSelector(new Random(42));

        Set<MapCandidate> drawn = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            drawn.add(selector.select(candidates));
        }

        assertEquals(Set.copyOf(candidates), drawn);
    }

    @Test
    void refusesAnEmptyList() {
        RandomMapSelector selector = new RandomMapSelector();

        assertThrows(IllegalArgumentException.class, () -> selector.select(List.of()));
    }

    private static MapCandidate candidate(Path root, String name) throws IOException {
        GameMap map = new GameMap(name, Pos.ZERO, Pos.ZERO, Set.of(), Set.of(), List.of(), null);
        return new MapCandidate(MapEntry.of(Files.createDirectories(root.resolve(name))), map);
    }
}
