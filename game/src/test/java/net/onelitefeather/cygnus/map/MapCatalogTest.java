package net.onelitefeather.cygnus.map;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.theevilreaper.aves.map.BaseMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapCatalogTest {

    @Test
    void findsEveryGameMapAndKeepsTheLobbyApart(@TempDir Path root) throws IOException {
        writeLobby(root);
        writeGameMap(root, "Granskoga");
        writeGameMap(root, "Withered Pines");

        MapCatalog catalog = MapCatalog.load(root);

        assertEquals(Path.of("lobby"), catalog.lobby().getDirectoryRoot().getFileName());
        assertEquals(Set.of("Granskoga", "Withered Pines"),
                Set.copyOf(catalog.candidates().stream().map(MapCandidate::name).toList()));
    }

    @Test
    void skipsAMapThatCannotBeRead(@TempDir Path root) throws IOException {
        writeLobby(root);
        writeGameMap(root, "Granskoga");
        Path broken = writeGameMap(root, "Broken");
        Files.writeString(broken.resolve("map.json"), "{ not json", StandardCharsets.UTF_8);

        MapCatalog catalog = MapCatalog.load(root);

        assertEquals(List.of("Granskoga"), catalog.candidates().stream().map(MapCandidate::name).toList());
    }

    @Test
    void failsWithoutAGameMapThatCanBeRead(@TempDir Path root) throws IOException {
        writeLobby(root);
        Path broken = writeGameMap(root, "Broken");
        Files.writeString(broken.resolve("map.json"), "{ not json", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> MapCatalog.load(root));
    }

    @Test
    void failsWithoutALobby(@TempDir Path root) throws IOException {
        writeGameMap(root, "Granskoga");

        assertThrows(IllegalStateException.class, () -> MapCatalog.load(root));
    }

    @Test
    void selectHandsTheCandidatesToTheSelector(@TempDir Path root) throws IOException {
        writeLobby(root);
        writeGameMap(root, "Granskoga");
        writeGameMap(root, "Withered Pines");
        MapCatalog catalog = MapCatalog.load(root);

        MapCandidate selected = catalog.select(candidates -> candidates.getLast());

        assertSame(catalog.candidates().getLast(), selected);
    }

    private static void writeLobby(Path root) throws IOException {
        writeMap(root, "lobby", new BaseMap("lobby", Pos.ZERO, List.of()));
    }

    private static Path writeGameMap(Path root, String name) throws IOException {
        return writeMap(root, name, new GameMap(name, Pos.ZERO, new Pos(1, 1, 1), Set.of(), Set.of(new Pos(2, 2, 2)), List.of(), null));
    }

    private static Path writeMap(Path root, String directory, BaseMap map) throws IOException {
        Path directoryRoot = root.resolve("game").resolve("maps").resolve(directory);
        Files.createDirectories(directoryRoot.resolve("region"));
        Files.writeString(directoryRoot.resolve("map.json"), GsonHelper.GSON.toJson(map), StandardCharsets.UTF_8);
        return directoryRoot;
    }
}
