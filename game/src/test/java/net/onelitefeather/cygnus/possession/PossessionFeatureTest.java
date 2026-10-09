package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.PossessionConfig;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionFeatureTest extends CygnusPlayerTestBase {

    private final AtomicLong clock = new AtomicLong();
    private final AtomicReference<Set<Player>> survivors = new AtomicReference<>(Set.of());
    private final FakeTarget target = new FakeTarget();

    /** A creek that is whatever the test says, and remembers what was done to it. */
    private static final class FakeTarget implements PossessionTarget {

        @Nullable Entity creek;
        @Nullable UUID possessedBy;
        int releases;

        @Override
        public @Nullable Entity possessable() {
            return this.creek;
        }

        @Override
        public void possess(UUID slender, double sightFactor) {
            this.possessedBy = slender;
        }

        @Override
        public void release() {
            this.possessedBy = null;
            this.releases++;
        }

        @Override
        public int sightRange() {
            return 10;
        }
    }

    private PossessionFeature feature(@Nullable Player slender) {
        PossessionFeature feature = new PossessionFeature(PossessionConfig.DEFAULT, true, this.target, this.survivors::get,
                () -> slender, this.clock::get);
        feature.started();
        return feature;
    }

    private static Player slender(Player player) {
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        return player;
    }

    private EntityCreature creek(Instance instance) {
        EntityCreature creek = new EntityCreature(EntityType.CREAKING);
        creek.setInstance(instance, new Pos(20, 40, 0)).join();
        this.target.creek = creek;
        return creek;
    }

    @Test
    @DisplayName("Possessing reveals the slender, moves his camera to the creek and lets nearby survivors glow")
    void possessing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = slender(connection.connect(instance, new Pos(0, 40, 0)));
        Player near = env.createPlayer(instance, new Pos(25, 40, 0));
        Player far = env.createPlayer(instance, new Pos(60, 40, 0));
        this.survivors.set(Set.of(near, far));
        EntityCreature creek = this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);

        assertTrue(feature.use(slender));

        assertFalse(VisibilityRules.isHidden(slender));
        assertTrue(slender.hasTag(Tags.POSSESSING));
        assertEquals(slender.getUuid(), this.target.possessedBy);
        List<CameraPacket> sent = cameras.collect();
        assertEquals(1, sent.size());
        assertEquals(creek.getEntityId(), sent.getFirst().cameraId());
        assertEquals(Set.of(near), feature.glowing());
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("After the maximum time he is hidden again, the camera is back and the full cooldown runs")
    void endsAfterTheMaximum(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = slender(connection.connect(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);

        this.clock.set(8_000L);
        feature.tick();

        assertNull(feature.possession());
        assertTrue(VisibilityRules.isHidden(slender));
        assertFalse(slender.hasTag(Tags.POSSESSING));
        assertNull(this.target.possessedBy);
        assertEquals(slender.getEntityId(), cameras.collect().getFirst().cameraId());
        assertTrue(feature.glowing().isEmpty());
        assertFalse(feature.use(slender), "the cooldown runs");
        this.clock.set(68_000L);
        assertTrue(feature.use(slender));
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Sneaking ends it early, and the cooldown is still the full one")
    void sneakingEndsEarly(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);

        this.clock.set(1_000L);
        feature.sneaked(slender);

        assertNull(feature.possession());
        this.clock.set(60_999L);
        assertFalse(feature.use(slender));
        this.clock.set(61_000L);
        assertTrue(feature.use(slender));
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A use that does not get into the creek costs nothing")
    void failedUseIsFree(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        PossessionFeature feature = this.feature(slender);

        slender.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        this.creek(instance);
        assertFalse(feature.use(slender), "only while hidden");

        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        this.target.creek = null;
        assertFalse(feature.use(slender), "not without a creek");

        this.creek(instance);
        assertTrue(feature.use(slender), "neither attempt started the cooldown");
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The creek going away ends it, with the full cooldown")
    void endsWhenTheCreekGoes(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);

        this.target.creek = null;
        feature.tick();

        assertNull(feature.possession());
        assertTrue(VisibilityRules.isHidden(slender));
        this.creek(instance);
        assertFalse(feature.use(slender), "the cooldown runs");
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Leaving the game cleans up without a cooldown")
    void leavingCleansUpWithoutCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);

        feature.left(slender);

        assertNull(feature.possession());
        assertNull(this.target.possessedBy);
        assertEquals(1, this.target.releases);
        assertFalse(slender.hasTag(Tags.POSSESSING));
        assertTrue(feature.glowing().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Only one slender can be in the creek, a second one is turned away for free")
    void oneAtATime(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        Player second = slender(env.createPlayer(instance, new Pos(2, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(first);
        feature.use(first);

        assertFalse(feature.use(second));

        Possession possession = feature.possession();
        assertNotNull(possession);
        assertSame(first, possession.slender());
        feature.sneaked(first);
        assertTrue(feature.use(second), "the turned away slender has no cooldown");
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Without a running round the item does nothing")
    void nothingOutsideTheRound(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.stop();

        assertFalse(feature.use(slender));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The round ending brings the camera back and hides the slender, without a cooldown")
    void roundEndWithoutCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player slender = slender(connection.connect(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);
        Collector<CameraPacket> cameras = connection.trackIncoming(CameraPacket.class);
        Collector<SetCooldownPacket> cooldowns = connection.trackIncoming(SetCooldownPacket.class);

        feature.stop();

        assertNull(feature.possession());
        assertTrue(VisibilityRules.isHidden(slender));
        assertFalse(slender.hasTag(Tags.POSSESSING));
        assertNull(this.target.possessedBy);
        assertEquals(slender.getEntityId(), cameras.collect().getFirst().cameraId());
        assertTrue(cooldowns.collect().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A slender who dies leaves the creek, with the full cooldown")
    void deathEndsIt(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(slender);
        feature.use(slender);

        feature.node().call(new PlayerDeathEvent(slender, null, null));

        assertNull(feature.possession());
        assertFalse(slender.hasTag(Tags.POSSESSING));
        assertFalse(feature.use(slender), "the cooldown runs");
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A new slender ends the old one's possession and gets the item")
    void newSlenderTakesOver(Env env) {
        Instance instance = env.createFlatInstance();
        Player old = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        Player successor = slender(env.createPlayer(instance, new Pos(2, 40, 0)));
        this.creek(instance);
        PossessionFeature feature = this.feature(old);
        feature.use(old);

        feature.node().call(new SlenderReviveEvent(successor));
        env.tick();

        assertNull(feature.possession());
        assertFalse(old.hasTag(Tags.POSSESSING));
        assertEquals(Items.POSSESSION_ITEM,
                successor.getInventory().getItemStack(Items.POSSESSION_SLOT).getTag(Tags.ITEM_TAG));
        assertTrue(feature.use(successor), "the new slender has no cooldown");
        feature.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A creek outside the slender's view is too far away, and trying costs nothing")
    void farCreekIsTurnedDown(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = slender(env.createPlayer(instance, new Pos(0, 40, 0)));
        EntityCreature creek = this.creek(instance);
        // Beyond the entity view distance, so the slender's client does not know the creek.
        creek.teleport(new Pos(200, 40, 0)).join();
        PossessionFeature feature = this.feature(slender);

        assertFalse(feature.use(slender));

        assertNull(feature.possession());
        assertTrue(VisibilityRules.isHidden(slender));
        assertFalse(slender.hasTag(Tags.POSSESSING));
        assertNull(this.target.possessedBy);
        assertEquals(1, this.target.releases, "the creek is let go again");
        creek.teleport(new Pos(20, 40, 0)).join();
        assertTrue(feature.use(slender), "the failed try started no cooldown");
        feature.stop();
        env.destroyInstance(instance, true);
    }
}
