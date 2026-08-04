package net.onelitefeather.cygnus.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.theevilreaper.aves.map.BaseMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Verifies that the maps of the game module are read through Falco instead of the chunk loader
 * Minestom ships with, for the lobby as well as for the game map.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
@ExtendWith(MicrotusExtension.class)
class GameMapProviderIntegrationTest {

    private static final String ARENA_NAME = "arena";

    @Test
    void testLobbyInstanceUsesFalcoChunkLoader(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);

        InstanceContainer lobbyInstance = (InstanceContainer) provider.getActiveInstance().get();
        assertNotNull(lobbyInstance);
        assertInstanceOf(FalcoAnvilLoader.class, lobbyInstance.getChunkLoader());

        provider.close();
        env.destroyInstance(lobbyInstance, true);
    }

    @Test
    void testGameInstanceUsesOwnFalcoChunkLoader(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);

        InstanceContainer lobbyInstance = (InstanceContainer) provider.getActiveInstance().get();
        provider.loadGameMap();
        provider.switchToGameMap();

        InstanceContainer gameInstance = (InstanceContainer) provider.getActiveInstance().get();
        assertNotSame(lobbyInstance, gameInstance);
        assertInstanceOf(FalcoAnvilLoader.class, gameInstance.getChunkLoader());
        assertNotSame(lobbyInstance.getChunkLoader(), gameInstance.getChunkLoader());
        assertNotNull(provider.getGameMap());

        provider.close();
        env.destroyInstance(gameInstance, true);
    }

    @Test
    void testCloseIsRepeatable(Env env, @TempDir Path root) throws IOException {
        GameMapProvider provider = createProvider(root);
        InstanceContainer lobbyInstance = (InstanceContainer) provider.getActiveInstance().get();

        provider.close();

        assertDoesNotThrow(provider::close);
        env.destroyInstance(lobbyInstance, true);
    }

    /**
     * Creates a map directory layout the provider accepts and returns a provider reading it.
     *
     * @param root the directory the {@code game/maps} tree is written below
     * @return the provider for the written maps
     * @throws IOException if the layout cannot be written
     */
    private GameMapProvider createProvider(Path root) throws IOException {
        Path maps = root.resolve("game").resolve("maps");
        writeMap(maps.resolve("lobby"), new BaseMap("lobby", Pos.ZERO, List.of()), false);
        writeMap(
                maps.resolve(ARENA_NAME),
                new GameMap(ARENA_NAME, Pos.ZERO, new Pos(1, 1, 1), Set.of(), Set.of(new Pos(2, 2, 2)), List.of()),
                true
        );
        return new GameMapProvider(root);
    }

    /**
     * Writes a map directory the way {@code MapFilters} expects it: a region directory next to a
     * {@code map.json}. The directory stays empty, so the loader simply finds no region file and
     * hands out no chunk — which is all this test needs from it.
     *
     * <p>The lobby is written in the layout used before 26.2 and the game map in the one 26.2
     * writes, so a single provider covers both.</p>
     *
     * @param directoryRoot   the world root of the map
     * @param map             the map data written to {@code map.json}
     * @param dimensionLayout whether the region directory is written below {@code dimensions}
     * @throws IOException if the layout cannot be written
     */
    private void writeMap(Path directoryRoot, BaseMap map, boolean dimensionLayout) throws IOException {
        Path regionDirectory = dimensionLayout
                ? directoryRoot.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("region")
                : directoryRoot.resolve("region");
        Files.createDirectories(regionDirectory);
        Files.writeString(directoryRoot.resolve("map.json"), GsonHelper.GSON.toJson(map), StandardCharsets.UTF_8);
    }
}
