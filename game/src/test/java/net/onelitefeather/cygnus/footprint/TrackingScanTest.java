package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingScanTest extends CygnusPlayerTestBase {

    private final AtomicLong clock = new AtomicLong();
    private final SurvivorTrackLog log = new SurvivorTrackLog(FootprintConfig.DEFAULT);
    private final FootprintSpawner spawner = new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
    private final TrackingScan scan = new TrackingScan(FootprintConfig.DEFAULT, this.log, this.spawner, this.clock::get);

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    /** Lets a survivor walk ten blocks along x at the given time, which records five points. */
    private void walked(UUID survivor, long time) {
        for (int x = 0; x < 10; x++) {
            this.log.moved(survivor, new Pos(x + 0.5, 40, 0.5), new Pos(x + 1.5, 40, 0.5), time);
        }
    }

    @Test
    @DisplayName("A scan shows recorded tracks to the slender only")
    void revealsToSlender(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 5.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Player survivor = env.createPlayer(instance, new Pos(3.5, 40, 5.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        walked(survivor.getUuid(), 0L);
        this.clock.set(10_000L);

        assertTrue(this.scan.use(slender));
        tick(env, 2);

        assertEquals(5, this.spawner.live().size());
        Footprint footprint = this.spawner.live().iterator().next();
        assertTrue(footprint.isShownTo(slender));
        assertFalse(footprint.isShownTo(survivor));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The newest seconds are left out")
    void leavesOutNewest(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 5.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        walked(UUID.randomUUID(), 8_000L);
        this.clock.set(10_000L);

        this.scan.use(slender);

        assertTrue(this.spawner.live().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A second use waits for the cooldown and gets a packet with it")
    void cooldown(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = connection.connect(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Collector<ServerPacket> packets = connection.trackIncoming();

        assertTrue(this.scan.use(slender));
        assertTrue(packets.collect().stream().anyMatch(packet -> packet instanceof SetCooldownPacket cooldown
                && cooldown.cooldownGroup().equals(TrackingScan.COOLDOWN_GROUP)
                && cooldown.cooldownTicks() == 120 * 20));

        this.clock.set(119_999L);
        assertFalse(this.scan.use(slender));

        this.clock.set(120_000L);
        assertTrue(this.scan.use(slender));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("An empty scan still starts the cooldown")
    void emptyScanCosts(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        assertTrue(this.scan.use(slender));
        this.clock.set(1_000L);

        assertFalse(this.scan.use(slender));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The remaining cooldown can be shown to a new slender")
    void showsRemainingCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        first.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        this.scan.use(first);
        this.clock.set(20_000L);
        TestConnection connection = env.createConnection();
        Player next = connection.connect(instance, new Pos(2.5, 40, 0.5));
        Collector<ServerPacket> packets = connection.trackIncoming();

        this.scan.showCooldown(next);

        assertTrue(packets.collect().stream().anyMatch(packet -> packet instanceof SetCooldownPacket cooldown
                && cooldown.cooldownTicks() == 100 * 20));
        env.destroyInstance(instance, true);
    }
}
