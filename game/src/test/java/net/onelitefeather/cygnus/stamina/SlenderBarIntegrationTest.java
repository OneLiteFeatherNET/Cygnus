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
import net.minestom.server.entity.attribute.Attribute;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
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
    void testActivationShowsFullBar(@NotNull Env env) {
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
    void testRegenerationStopsAtFullBar(@NotNull Env env) {
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
    void testHalfDrainedTickShowsAHalfTileInsteadOfDroppingAWholeTile(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING, currentTime starts at 16.0

        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);
        slenderBar.consume(); // one 0.5 tick: currentTime becomes 15.5

        collector.assertSingle(packet -> {
            TextComponent root = assertInstanceOf(TextComponent.class, packet.text());
            assertEquals("▋".repeat(15), root.content(), "15 full tiles should stay filled after only half a tile drained");
            assertEquals(2, root.children().size(), "a half-drained tile needs its own segment next to the empty segment");
            TextComponent half = assertInstanceOf(TextComponent.class, root.children().get(0));
            assertEquals("▍", half.content(), "the 16th tile should render as a half tile, not disappear entirely");
            TextComponent empty = assertInstanceOf(TextComponent.class, root.children().get(1));
            assertEquals("", empty.content(), "no fully empty tiles yet after just one tick");
        });

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testCannotReactivateTooEarly(@NotNull Env env) {
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
    void testCanReactivateAtThreshold(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // 34 ticks to fully drain and auto-switch to REGENERATING at currentTime == -0.5,
        // then 21 more ticks of +0.5 regeneration land currentTime exactly on 10.0.
        for (int i = 0; i < 55; i++) {
            slenderBar.consume();
        }

        boolean reactivated = slenderBar.changeStatus();

        assertTrue(reactivated, "the javadoc says regeneration must \"reach\" the threshold - " +
                "landing exactly on it should be sufficient, not just exceeding it");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testAutoDepletionPlaysTeleportSound(@NotNull Env env) {
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

    @Test
    void testSlenderBarStateTransitionsUseModifiers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();

        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue(), 0.001);

        slenderBar.changeStatus(); // enter DRAINING
        assertTrue(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().stream().anyMatch(m -> m.id().equals(AttributeHelper.SLENDER_DRAINING_SPEED_KEY)));
        assertEquals(0.0669, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.001);

        slenderBar.changeStatus(); // enter REGENERATING
        assertFalse(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().stream().anyMatch(m -> m.id().equals(AttributeHelper.SLENDER_DRAINING_SPEED_KEY)));
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue(), 0.001);

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }


}

