package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionCooldownTest extends CygnusPlayerTestBase {

    private final AtomicLong clock = new AtomicLong();
    private final PossessionCooldown cooldown = new PossessionCooldown(60, this.clock::get);

    @Test
    @DisplayName("A started cooldown blocks until it is over and tells the client how long")
    void blocksUntilOver(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SetCooldownPacket> packets = connection.trackIncoming(SetCooldownPacket.class);

        assertTrue(this.cooldown.isReady(slender));
        this.cooldown.start(slender);

        assertFalse(this.cooldown.isReady(slender));
        List<SetCooldownPacket> sent = packets.collect();
        assertEquals(1, sent.size());
        assertEquals(PossessionCooldown.COOLDOWN_GROUP, sent.getFirst().cooldownGroup());
        assertEquals(60 * 20, sent.getFirst().cooldownTicks());
        this.clock.set(59_999L);
        assertFalse(this.cooldown.isReady(slender));
        this.clock.set(60_000L);
        assertTrue(this.cooldown.isReady(slender));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The cooldown belongs to one player, and a reset clears it")
    void perPlayerAndReset(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance, new Pos(0, 40, 0));
        Player second = env.createPlayer(instance, new Pos(2, 40, 0));

        this.cooldown.start(first);

        assertFalse(this.cooldown.isReady(first));
        assertTrue(this.cooldown.isReady(second));
        this.cooldown.reset();
        assertTrue(this.cooldown.isReady(first));
        env.destroyInstance(instance, true);
    }
}
