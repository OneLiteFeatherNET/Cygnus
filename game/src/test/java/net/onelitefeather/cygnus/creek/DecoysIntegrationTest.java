package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoysIntegrationTest extends CygnusPlayerTestBase {

    private static final long REVEAL_MILLIS = 700L;
    /** Looking towards positive Z, straight at {@link #DECOY_SPOT}. */
    private static final Pos VIEWER_POS = new Pos(0, 40, 0, 0, 0);
    private static final Pos DECOY_SPOT = new Pos(0, 40, 10);

    private final List<CreekBody> spawned = new ArrayList<>();

    private Decoys decoys() {
        CreekSight sight = new CreekSight(30, CreekConfig.DEFAULT.sight().viewAngle());
        return new Decoys((instance, spot) -> {
            CreekBody body = CreakingBody.spawn(instance, spot);
            this.spawned.add(body);
            return body;
        }, sight, REVEAL_MILLIS, () -> 0L);
    }

    private static SurvivorSnapshot looking(Player viewer, boolean atTheDecoy) {
        Pos position = atTheDecoy ? VIEWER_POS : VIEWER_POS.withYaw(180.0F);
        return new SurvivorSnapshot(List.of(viewer), List.of(new SurvivorView(viewer.getUuid(), position, 0.0D, false)));
    }

    private boolean removed() {
        return this.spawned.getFirst().entity().isRemoved();
    }

    @Test
    @DisplayName("Only the survivor it was made for sees the doppelganger")
    void decoyShowsToItsViewerOnly(Env env) {
        Instance instance = env.createFlatInstance();
        Player viewer = env.createConnection().connect(instance, VIEWER_POS);
        Player other = env.createConnection().connect(instance, new Pos(5, 40, 0));

        this.decoys().spawn(viewer, DECOY_SPOT);

        assertTrue(this.spawned.getFirst().isVisibleTo(viewer.getUuid()));
        assertFalse(this.spawned.getFirst().isVisibleTo(other.getUuid()));
    }

    @Test
    @DisplayName("Looked at long enough, the doppelganger crumbles to leaves with the scare sound")
    void crumblesAfterALongLook(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player viewer = connection.connect(instance, VIEWER_POS);
        Decoys decoys = this.decoys();
        decoys.spawn(viewer, DECOY_SPOT);
        Collector<ParticlePacket> particles = connection.trackIncoming(ParticlePacket.class);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        decoys.tick(looking(viewer, true), 0L);
        decoys.tick(looking(viewer, true), REVEAL_MILLIS - 1L);
        assertFalse(this.removed());

        decoys.tick(looking(viewer, true), REVEAL_MILLIS);

        assertTrue(this.removed());
        assertFalse(particles.collect().isEmpty());
        assertTrue(sounds.collect().stream().anyMatch(packet -> packet.soundEvent() == SoundEvent.ENTITY_CREAKING_ACTIVATE));
    }

    @Test
    @DisplayName("Glances that are each too short do not add up")
    void aGlanceIsNotEnough(Env env) {
        Instance instance = env.createFlatInstance();
        Player viewer = env.createConnection().connect(instance, VIEWER_POS);
        Decoys decoys = this.decoys();
        decoys.spawn(viewer, DECOY_SPOT);

        decoys.tick(looking(viewer, true), 0L);
        decoys.tick(looking(viewer, true), 600L);
        decoys.tick(looking(viewer, false), 650L);
        decoys.tick(looking(viewer, true), 700L);
        decoys.tick(looking(viewer, true), 1300L);

        assertFalse(this.removed());
    }

    @Test
    @DisplayName("Once its survivor is gone, so is the doppelganger")
    void decoyGoesWithItsViewer(Env env) {
        Instance instance = env.createFlatInstance();
        Player viewer = env.createConnection().connect(instance, VIEWER_POS);
        Decoys decoys = this.decoys();
        decoys.spawn(viewer, DECOY_SPOT);

        decoys.tick(new SurvivorSnapshot(List.of(), List.of()), 0L);

        assertTrue(this.removed());
    }

    @Test
    @DisplayName("Never looked at, the doppelganger is gone once its time is up")
    void expiresAfterItsLifetime(Env env) {
        Instance instance = env.createFlatInstance();
        Player viewer = env.createConnection().connect(instance, VIEWER_POS);
        Decoys decoys = this.decoys();
        decoys.spawn(viewer, DECOY_SPOT);

        decoys.tick(looking(viewer, false), Decoys.LIFETIME_MILLIS - 1L);
        assertFalse(this.removed());
        decoys.tick(looking(viewer, false), Decoys.LIFETIME_MILLIS);

        assertTrue(this.removed());
    }

    @Test
    @DisplayName("Clearing takes every doppelganger out of the world")
    void clearRemovesDecoys(Env env) {
        Instance instance = env.createFlatInstance();
        Player viewer = env.createConnection().connect(instance, VIEWER_POS);
        Decoys decoys = this.decoys();
        decoys.spawn(viewer, DECOY_SPOT);

        decoys.clear();

        assertTrue(this.removed());
    }
}
