package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that a setup instance reads its world through Falco instead of the chunk loader Minestom
 * ships with, and that the loader is released again when the setup is reset.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
@ExtendWith(MicrotusExtension.class)
class InstanceSetupDataIntegrationTest {

    @Test
    void testInstanceUsesFalcoChunkLoader(Env env, @TempDir Path worldRoot) {
        TestSetupData data = new TestSetupData(MapEntry.of(worldRoot));
        data.loadData();

        assertNotNull(data.instance);
        assertNotNull(data.chunkLoader);
        assertInstanceOf(FalcoAnvilLoader.class, data.instance.getChunkLoader());
        assertSame(data.chunkLoader, data.instance.getChunkLoader());

        env.destroyInstance(data.instance, true);
    }

    @Test
    void testResetClosesChunkLoader(Env env, @TempDir Path worldRoot) {
        TestSetupData data = new TestSetupData(MapEntry.of(worldRoot));
        data.loadData();

        assertNotNull(data.instance);
        data.reset();

        assertNull(data.chunkLoader, "The chunk loader has to be released when the setup is reset");
    }

    @Test
    void testResetWithoutInstanceIsHarmless(@TempDir Path worldRoot) {
        TestSetupData data = new TestSetupData(MapEntry.of(worldRoot));

        data.reset();

        assertNull(data.instance);
        assertNull(data.chunkLoader);
    }

    /**
     * Minimal {@link InstanceSetupData} which only creates the instance, so the test observes the
     * chunk loader handling of the base class and nothing else.
     */
    private static final class TestSetupData extends InstanceSetupData {

        private TestSetupData(MapEntry mapEntry) {
            super(UUID.randomUUID(), mapEntry, BossBar.Color.WHITE);
        }

        @Override
        public void loadData() {
            this.createInstance();
        }

        @Override
        public void save() {
            // Not part of what this test observes
        }

        @Override
        public void openInventory(InventoryTarget target) {
            // Not part of what this test observes
        }

        @Override
        public void triggerUpdate(InventoryTarget target) {
            // Not part of what this test observes
        }

        @Override
        public void setPosition(MapDataCategory category, Player player) {
            // Not part of what this test observes
        }

        @Override
        public void handleDataDelete(MapDataCategory category) {
            // Not part of what this test observes
        }

        @Override
        public BaseMapBuilder getMapBuilder() {
            return BaseMap.builder();
        }
    }
}
