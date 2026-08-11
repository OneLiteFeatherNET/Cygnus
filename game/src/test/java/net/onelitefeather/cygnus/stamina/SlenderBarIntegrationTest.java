package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that the {@link SlenderBar} renders its progress bar correctly.
 */
class SlenderBarIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testActivatingSlenderBarShowsFullProgressBarImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();

        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);
        slenderBar.changeStatus();

        collector.assertSingle(packet -> {
            Component text = packet.text();
            TextComponent root = assertInstanceOf(TextComponent.class, text);
            assertEquals("▋".repeat(16), root.content(), "the bar should be fully filled right when draining starts");
            assertEquals(1, root.children().size());
            TextComponent empty = assertInstanceOf(TextComponent.class, root.children().getFirst());
            assertEquals("", empty.content(), "no segment should be missing right when draining starts");
        });

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testRegenerationStopsExactlyAtFullBar(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus();

        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 80; i++) {
                slenderBar.consume();
            }
        }, "a full drain-then-regenerate cycle must not overshoot the bar and crash");

        List<ActionBarPacket> packets = collector.collect();
        assertFalse(packets.isEmpty());
        ActionBarPacket last = packets.get(packets.size() - 1);
        TextComponent root = assertInstanceOf(TextComponent.class, last.text());
        assertEquals("▋".repeat(16), root.content(), "regeneration should stop exactly at a full bar");
        TextComponent empty = assertInstanceOf(TextComponent.class, root.children().getFirst());
        assertEquals("", empty.content());

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testCannotReactivateBeforeSufficientRegeneration(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // fully drain so the bar enters REGENERATING at its lowest point
        for (int i = 0; i < 34; i++) {
            slenderBar.consume();
        }

        boolean reactivated = slenderBar.changeStatus();

        assertFalse(reactivated, "should not be able to hide again before stamina has sufficiently regenerated");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testAutomaticDepletionPlaysTeleportSoundToNearbyPlayers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        TestConnection survivorConnection = env.createConnection();
        Player nearbySurvivor = survivorConnection.connect(instance);
        nearbySurvivor.teleport(player.getPosition()).join();
        env.tick();

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // the teleport sound plays to nearby SURVIVORS, not to the slender player themselves
        Collector<ServerPacket> collector = survivorConnection.trackIncoming();
        // fully drain so the bar automatically switches to REGENERATING
        for (int i = 0; i < 34; i++) {
            slenderBar.consume();
        }

        assertTrue(soundWasSent(collector),
                "nearby players should hear the teleport sound when draining runs out on its own too, " +
                        "not just when it's manually cancelled");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    private static boolean soundWasSent(Collector<ServerPacket> collector) {
        return collector.collect().stream().anyMatch(packet -> packet.getClass().getSimpleName().contains("Sound"));
    }
}
