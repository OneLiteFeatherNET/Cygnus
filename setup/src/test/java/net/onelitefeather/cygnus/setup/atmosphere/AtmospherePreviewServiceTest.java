package net.onelitefeather.cygnus.setup.atmosphere;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.dimension.StaticDimensionPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the state machine the preview service runs, which decides where a builder lands after each
 * of the two configuration phases a preview costs.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
@ExtendWith(MicrotusExtension.class)
class AtmospherePreviewServiceTest {

    private static final MapAtmosphere ATMOSPHERE = MapAtmosphere.from(StaticDimensionPreset.DENSE_FOG);

    @Test
    void reportsNoPreviewForAnUntouchedPlayer(Env env) {
        AtmospherePreviewService service = new AtmospherePreviewService();
        Instance origin = env.createFlatInstance();
        Player player = env.createPlayer(origin);

        assertFalse(service.isPreviewing(player));
        assertNull(service.pendingInstance(player));

        env.destroyInstance(origin, true);
    }

    @Test
    void sendsThePlayerIntoAnInstanceOnItsOwnDimension(Env env, @TempDir Path world) throws IOException {
        AtmospherePreviewService service = new AtmospherePreviewService();
        Instance origin = env.createFlatInstance();
        Player player = env.createPlayer(origin);
        Files.createDirectories(world.resolve("region"));

        service.preview(player, ATMOSPHERE, world);

        Instance preview = service.pendingInstance(player);
        assertTrue(service.isPreviewing(player));
        assertNotSame(origin, preview);
        assertNotSame(DimensionType.OVERWORLD, preview.getDimensionType());
        assertEquals("cygnus", preview.getDimensionType().key().namespace());
        assertTrue(preview.getDimensionType().key().value().startsWith("preview/"));

        service.discard(player);
        env.destroyInstance(origin, true);
    }

    @Test
    void aSecondPreviewGetsItsOwnDimensionAndKeepsTheOrigin(Env env, @TempDir Path world) throws IOException {
        AtmospherePreviewService service = new AtmospherePreviewService();
        Instance origin = env.createFlatInstance();
        Player player = env.createPlayer(origin);
        Files.createDirectories(world.resolve("region"));

        service.preview(player, ATMOSPHERE, world);
        Instance first = service.pendingInstance(player);

        service.preview(player, MapAtmosphere.from(StaticDimensionPreset.VERY_DARK), world);
        Instance second = service.pendingInstance(player);

        assertNotSame(first, second);
        assertNotSame(first.getDimensionType(), second.getDimensionType());

        service.leave(player);
        assertSame(origin, service.pendingInstance(player), "leave() must aim back at the original instance");

        service.discard(player);
        env.destroyInstance(origin, true);
    }

    @Test
    void leavingAimsBackAtTheOriginUntilTheSpawnArrives(Env env, @TempDir Path world) throws IOException {
        AtmospherePreviewService service = new AtmospherePreviewService();
        Instance origin = env.createFlatInstance();
        Player player = env.createPlayer(origin);
        Files.createDirectories(world.resolve("region"));

        service.preview(player, ATMOSPHERE, world);
        service.leave(player);

        assertTrue(service.isPreviewing(player), "the session has to outlive the move back");
        assertSame(origin, service.pendingInstance(player));

        service.handleSpawn(player);

        assertFalse(service.isPreviewing(player));
        assertNull(service.pendingInstance(player));

        env.destroyInstance(origin, true);
    }

    @Test
    void spawningDuringAPreviewDoesNotEndIt(Env env, @TempDir Path world) throws IOException {
        AtmospherePreviewService service = new AtmospherePreviewService();
        Instance origin = env.createFlatInstance();
        Player player = env.createPlayer(origin);
        Files.createDirectories(world.resolve("region"));

        service.preview(player, ATMOSPHERE, world);
        service.handleSpawn(player);

        assertTrue(service.isPreviewing(player), "arriving in the preview must not close it");

        service.discard(player);
        env.destroyInstance(origin, true);
    }
}
