package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.minestom.server.entity.attribute.Attribute;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that the {@link SlenderBar} renders its progress bar correctly.
 */
class SlenderBarIntegrationTest extends CygnusPlayerTestBase {

    private Instance instance;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(Env env) {
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testActivationShowsFullBar(@NotNull Env env) {
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
    }

    @Test
    @DisplayName("Even his first appearance of the round is heard")
    void testTheFirstAppearanceIsHeard(@NotNull Env env) {
        CygnusPlayer slender = (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(10, 40, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();
        Collector<SoundEffectPacket> sounds = survivorConnection.trackIncoming(SoundEffectPacket.class);

        slenderBar.changeStatus(); // READY -> DRAINING, the bar is still full

        sounds.assertSingle(packet -> assertEquals(SlenderBarHelper.SPAWN.name(), packet.soundEvent().key()));
        slenderBar.stop();
    }

    @Test
    void testRegenerationStopsAtFullBar(@NotNull Env env) {
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
    }

    @Test
    void testHalfDrainedTickShowsAHalfTileInsteadOfDroppingAWholeTile(@NotNull Env env) {
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
    }

    @Test
    void testCannotReactivateTooEarly(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // fully drain so the bar enters REGENERATING at its lowest point
        for (int i = 0; i < 32; i++) {
            slenderBar.consume();
        }

        boolean reactivated = slenderBar.changeStatus();

        assertFalse(reactivated, "should not be able to hide again before stamina has sufficiently regenerated");

        slenderBar.stop();
    }

    @Test
    void testCanReactivateAtThreshold(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);

        // Without the reappear cooldown, so only the regeneration threshold is under test
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player,
                new StaminaConfig(0.3D, 1.25D, 0, StaminaConfig.DEFAULT_SLENDER_DAMAGE_RANGE), System::currentTimeMillis);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // 32 ticks to fully drain and auto-switch to REGENERATING at currentTime == 0,
        // then 20 more ticks of +0.5 regeneration land currentTime exactly on 10.0.
        for (int i = 0; i < 52; i++) {
            slenderBar.consume();
        }

        boolean reactivated = slenderBar.changeStatus();

        assertTrue(reactivated, "the javadoc says regeneration must \"reach\" the threshold - " +
                "landing exactly on it should be sufficient, not just exceeding it");

        slenderBar.stop();
    }

    @Test
    void testAutoDepletionPlaysTeleportSound(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        TestConnection survivorConnection = env.createConnection();
        Player nearbySurvivor = survivorConnection.connect(instance);
        nearbySurvivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        nearbySurvivor.teleport(player.getPosition()).join();
        env.tick();

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // the teleport sound plays to nearby SURVIVORS, not to the slender player themselves
        Collector<ServerPacket> collector = survivorConnection.trackIncoming();
        // fully drain so the bar automatically switches to REGENERATING
        for (int i = 0; i < 32; i++) {
            slenderBar.consume();
        }

        assertTrue(soundWasSent(collector),
                "nearby players should hear the teleport sound when draining runs out on its own too, " +
                        "not just when it's manually cancelled");

        slenderBar.stop();
    }

    private static boolean soundWasSent(Collector<ServerPacket> collector) {
        return collector.collect().stream().anyMatch(packet -> packet.getClass().getSimpleName().contains("Sound"));
    }

    @Test
    void testSlenderBarStateTransitionsUseModifiers(@NotNull Env env) {
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
    }

    @Test
    @DisplayName("Once hidden, the slender cannot appear again before the cooldown ran out")
    void reappearWaitsForTheCooldown(@NotNull Env env) {
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance);
        AtomicLong clock = new AtomicLong();
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player, StaminaConfig.DEFAULT, clock::get);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING
        slenderBar.changeStatus(); // DRAINING -> REGENERATING at 0 ms, the bar is still nearly full

        clock.set(4_999);
        assertFalse(slenderBar.changeStatus());

        clock.set(5_000);
        assertTrue(slenderBar.changeStatus());

        slenderBar.stop();
    }

    @Test
    @DisplayName("A bar that regenerated to full inside the cooldown does not skip it")
    void readyBarStillWaitsForTheCooldown(@NotNull Env env) {
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance);
        AtomicLong clock = new AtomicLong();
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player, StaminaConfig.DEFAULT, clock::get);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING
        slenderBar.consume();      // 15.5 of 16
        slenderBar.changeStatus(); // DRAINING -> REGENERATING
        slenderBar.consume();      // full again
        slenderBar.consume();      // READY

        clock.set(1_000);

        assertFalse(slenderBar.changeStatus());

        slenderBar.stop();
    }

    @Test
    @DisplayName("The slender hides the moment his bar runs dry")
    void hidesWhenTheBarRunsDry(@NotNull Env env) {
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING, 16 units

        for (int i = 0; i < 31; i++) {
            slenderBar.consume();
        }
        assertEquals(SlenderBarHelper.VISIBLE, player.getTag(Tags.HIDDEN), "half a unit is still left");

        slenderBar.consume(); // the last half unit

        assertEquals(SlenderBarHelper.HIDDEN, player.getTag(Tags.HIDDEN));

        slenderBar.stop();
    }

    @Test
    @DisplayName("Hiding puts the eye on cooldown for the longer of both waits")
    void hidingSendsTheLongerWaitAsCooldown(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING

        // Hiding at 15.5 of 16: the bar is past the threshold, so the 5 s reappear cooldown counts
        slenderBar.consume();
        Collector<SetCooldownPacket> early = connection.trackIncoming(SetCooldownPacket.class);
        slenderBar.changeStatus();
        early.assertSingle(packet -> {
            assertEquals(SlenderBar.COOLDOWN_GROUP, packet.cooldownGroup());
            assertEquals(100, packet.cooldownTicks());
        });

        slenderBar.stop();
    }

    @Test
    @DisplayName("A bar that ran dry shows the time until it reaches the threshold again")
    void runningDrySendsTheRegenerationTime(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING
        for (int i = 0; i < 31; i++) {
            slenderBar.consume();
        }

        Collector<SetCooldownPacket> collector = connection.trackIncoming(SetCooldownPacket.class);
        slenderBar.consume(); // runs dry

        // 0 to 10 units at 0.5 every 500 ms takes 10 s, longer than the 5 s reappear cooldown
        collector.assertSingle(packet -> assertEquals(200, packet.cooldownTicks()));

        slenderBar.stop();
    }

    @Test
    @DisplayName("The bar does not refill while the slender looks through the creek")
    void possessionPausesRegeneration(@NotNull Env env) {
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance);
        // Without the reappear cooldown, so only the regeneration threshold is under test
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player,
                new StaminaConfig(0.3D, 1.25D, 0, StaminaConfig.DEFAULT_SLENDER_DAMAGE_RANGE), System::currentTimeMillis);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING
        for (int i = 0; i < 32; i++) {
            slenderBar.consume(); // runs dry, REGENERATING at 0
        }

        player.setTag(Tags.POSSESSING, true);
        for (int i = 0; i < 20; i++) {
            slenderBar.consume();
        }
        player.removeTag(Tags.POSSESSING);

        assertFalse(slenderBar.changeStatus(), "20 ticks inside the creek must not count towards the threshold");

        for (int i = 0; i < 20; i++) {
            slenderBar.consume();
        }
        assertTrue(slenderBar.changeStatus(), "once back, the bar refills as usual");

        slenderBar.stop();
    }

    @Test
    @DisplayName("Leaving the creek sends the eye the wait that is still left")
    void leavingThePossessionResendsTheCooldown(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        AtomicLong clock = new AtomicLong();
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player, StaminaConfig.DEFAULT, clock::get);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING
        for (int i = 0; i < 32; i++) {
            slenderBar.consume(); // runs dry, REGENERATING at 0
        }

        player.setTag(Tags.POSSESSING, true);
        slenderBar.consume();
        clock.set(8_000);
        player.removeTag(Tags.POSSESSING);

        Collector<SetCooldownPacket> collector = connection.trackIncoming(SetCooldownPacket.class);
        slenderBar.consume();

        // Still 0 of 10 units, so the full 10 s refill is left, longer than the expired reappear cooldown
        collector.assertSingle(packet -> assertEquals(200, packet.cooldownTicks()));

        slenderBar.stop();
    }

    @Test
    @DisplayName("Stopping the bar takes the cooldown off the eye")
    void stopClearsTheCooldown(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus();
        slenderBar.changeStatus();

        Collector<SetCooldownPacket> collector = connection.trackIncoming(SetCooldownPacket.class);
        slenderBar.stop();

        collector.assertSingle(packet -> assertEquals(0, packet.cooldownTicks()));
    }

    @Test
    @DisplayName("Stopping the bar mid drain takes his blindness, slowness and sprint block off")
    void testStopWhileDrainingLiftsTheDrainEffects(@NotNull Env env) {
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(player);
        slenderBar.start();
        slenderBar.changeStatus(); // READY -> DRAINING, the round ends while he hunts

        slenderBar.stop();

        assertFalse(player.getActiveEffects().stream().anyMatch(timed -> timed.potion().effect() == PotionEffect.BLINDNESS),
                "the blindness must not outlive the round");
        assertFalse(player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().stream().anyMatch(m -> m.id().equals(AttributeHelper.SLENDER_DRAINING_SPEED_KEY)));
        assertFalse(player.hasBlockedSprinting());
    }

    @Test
    @DisplayName("The first tick of an appearance does not swing yet")
    void firstTickOfAnAppearanceDoesNotSwing(@NotNull Env env) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        CygnusPlayer survivor = survivor(env, new Pos(1, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();

        bar.consume();

        assertEquals(100.0D, survivor.vitality().points(), DELTA, "a survivor next to him gets a moment to react");
        bar.stop();
    }

    @Test
    @DisplayName("Every second tick swings, one second apart")
    void swingsOnEverySecondTick(@NotNull Env env) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        CygnusPlayer survivor = survivor(env, new Pos(1, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();

        for (int i = 0; i < 4; i++) {
            bar.consume();
        }

        assertEquals(100.0D - 2 * HIT_POINTS, survivor.vitality().points(), DELTA, "four ticks are two swings");
        bar.stop();
    }

    @Test
    @DisplayName("A new appearance waits one second again before the first swing")
    void reappearingWaitsOneSecondAgain(@NotNull Env env) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        CygnusPlayer survivor = survivor(env, new Pos(1, 40, 0));
        AtomicLong clock = new AtomicLong();
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, clock::get, HIT);
        bar.start();
        bar.changeStatus();
        bar.consume();
        bar.changeStatus();
        clock.addAndGet(StaminaConfig.DEFAULT.slenderReappearCooldownSeconds() * 1000L);
        assertTrue(bar.changeStatus(), "he may appear again once the cooldown is over");

        bar.consume();

        assertEquals(100.0D, survivor.vitality().points(), DELTA, "the tick before hiding must not count towards the new appearance");
        bar.stop();
    }

    @Test
    @DisplayName("The swing on the tick the bar runs dry still lands")
    void swingLandsOnTheTickTheBarRunsDry(@NotNull Env env) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        CygnusPlayer survivor = survivor(env, new Pos(1, 40, 0));
        survivor.vitality().setUp(1000);
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        int ticks = (int) (SlenderBar.MAX_TIME / SlenderBar.TIME_STEP);

        for (int i = 0; i < ticks; i++) {
            bar.consume();
        }

        assertEquals(StaminaBar.State.REGENERATING, bar.state, "the bar ran dry");
        assertEquals(1100.0D - (ticks / SlenderBar.TICKS_PER_SWING) * HIT_POINTS, survivor.vitality().points(), DELTA,
                "the last tick is a swing tick and its swing lands before he hides");
    }

    @Test
    @DisplayName("A miss plays the swoosh to the slender and to nobody else")
    void missPlaysTheSwooshToTheSlenderOnly(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(1, 40, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, MISS);
        bar.start();
        bar.changeStatus();
        bar.consume();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);
        Collector<EntitySoundEffectPacket> survivorSounds = survivorConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();

        slenderSounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_ATTACK_NODAMAGE, packet.soundEvent()));
        survivorSounds.assertEmpty();
        bar.stop();
    }

    @Test
    @DisplayName("A hit plays the hit sound instead of the swoosh")
    void hitPlaysNoSwoosh(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivor(env, new Pos(1, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        bar.consume();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();

        slenderSounds.assertSingle(packet -> assertEquals(SlenderBar.HIT.name(), packet.soundEvent().key()));
        bar.stop();
    }

    @Test
    @DisplayName("Missing two survivors with one swing plays one swoosh")
    void twoMissesPlayOneSwoosh(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivor(env, new Pos(1, 40, 0));
        survivor(env, new Pos(-1, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, MISS);
        bar.start();
        bar.changeStatus();
        bar.consume();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();

        slenderSounds.assertSingle(packet -> assertEquals(SoundEvent.ENTITY_PLAYER_ATTACK_NODAMAGE, packet.soundEvent()));
        bar.stop();
    }

    @Test
    @DisplayName("A swing that hits one survivor and misses another plays only the hit sound")
    void hitAndMissPlayOnlyTheHitSound(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        CygnusPlayer calm = survivor(env, new Pos(1, 40, 0));
        CygnusPlayer terrified = survivor(env, new Pos(-1, 40, 0));
        // Every draw is 0.1: a hit for the calm one (0.1 >= 0.05), a miss for the terrified one (0.1 < 0.15).
        SlenderBar bar = (SlenderBar) StaminaFactory.createSlenderStamina(slender, StaminaConfig.DEFAULT, () -> 0L,
                uuid -> uuid.equals(terrified.getUuid()) ? 1.0D : 0.0D, new SlenderSwing(() -> new FixedRandom(0.1D)));
        bar.start();
        bar.changeStatus();
        bar.consume();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();

        assertTrue(calm.vitality().points() < 100.0D, "the calm survivor was hit");
        assertEquals(100.0D, terrified.vitality().points(), DELTA, "the terrified survivor was missed");
        slenderSounds.assertSingle(packet -> assertEquals(SlenderBar.HIT.name(), packet.soundEvent().key()));
        bar.stop();
    }

    @Test
    void testDrainDamageDoesNotChangeTheBarSpeed(@NotNull Env env) {
        TestConnection connection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) connection.connect(instance, new net.minestom.server.coordinate.Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();
        slenderBar.changeStatus();
        Collector<ActionBarPacket> collector = connection.trackIncoming(ActionBarPacket.class);

        slenderBar.consume();

        collector.assertSingle(packet -> {
            TextComponent root = assertInstanceOf(TextComponent.class, packet.text());
            assertEquals("▋".repeat(15), root.content(), "the bar must still move by 0.5 per tick");
            assertEquals("▍", assertInstanceOf(TextComponent.class, root.children().get(0)).content());
        });
        slenderBar.stop();
    }

    @Test
    void testDrainDoesNotHurtTheSlenderOrANonSurvivor(@NotNull Env env) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        Player spectator = env.createConnection().connect(instance, new Pos(1, 40, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        SlenderBar slenderBar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        slenderBar.start();
        slenderBar.changeStatus();

        slenderBar.consume();
        slenderBar.consume();

        assertEquals(20F, slender.getHealth(), 0.001F, "the slender must not hurt himself");
        assertEquals(20F, spectator.getHealth(), 0.001F, "non-survivors must stay untouched");
        slenderBar.stop();
    }

    @Test
    @DisplayName("A survivor 3.5 blocks away is hit with the default range")
    void survivorInsideTheDefaultRangeIsHurt(@NotNull Env env) {
        CygnusPlayer survivor = drainSwing(env, instance, StaminaConfig.DEFAULT, 3.5D);

        assertEquals(100.0D - HIT_POINTS, survivor.vitality().points(), DELTA, "one swing lands");
    }

    @Test
    @DisplayName("A survivor 4.5 blocks away is left alone with the default range")
    void survivorOutsideTheDefaultRangeIsUntouched(@NotNull Env env) {
        CygnusPlayer survivor = drainSwing(env, instance, StaminaConfig.DEFAULT, 4.5D);

        assertEquals(100.0D, survivor.vitality().points(), DELTA);
    }

    @Test
    @DisplayName("A configured range of 2 leaves a survivor 3 blocks away untouched")
    void configuredRangeIsHonoured(@NotNull Env env) {
        StaminaConfig narrow = new StaminaConfig(0.3D, 1.25D, 5, 2);
        CygnusPlayer survivor = drainSwing(env, instance, narrow, 3.0D);

        assertEquals(100.0D, survivor.vitality().points(), DELTA);
    }

    @Test
    @DisplayName("Only the slender hears the hit sound, once per swing however many survivors he hurts")
    void hitSoundPlaysOnceToTheSlender(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        TestConnection survivorConnection = env.createConnection();
        Player first = survivorConnection.connect(instance, new Pos(1, 40, 0));
        first.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        survivor(env, new Pos(-1, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        bar.consume();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);
        Collector<EntitySoundEffectPacket> survivorSounds = survivorConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();

        slenderSounds.assertSingle(packet -> assertEquals(SlenderBar.HIT.name(), packet.soundEvent().key()));
        survivorSounds.assertEmpty();
        bar.stop();
    }

    @Test
    @DisplayName("Every swing that hits plays the hit sound, however close the swings fall")
    void everyHittingSwingPlaysTheHitSound(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivor(env, new Pos(1, 40, 0));
        // The clock stands still, as if the scheduler ran the second swing early.
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);

        for (int i = 0; i < 2 * SlenderBar.TICKS_PER_SWING; i++) {
            bar.consume();
        }

        List<EntitySoundEffectPacket> sounds = slenderSounds.collect();
        assertEquals(2, sounds.size(), "two swings are two hit sounds");
        sounds.forEach(packet -> assertEquals(SlenderBar.HIT.name(), packet.soundEvent().key()));
        bar.stop();
    }

    @Test
    @DisplayName("Without a survivor in range the slender hears no hit sound")
    void noHitSoundWithoutASurvivorInRange(@NotNull Env env) {
        TestConnection slenderConnection = env.createConnection();
        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivor(env, new Pos(10, 40, 0));
        SlenderBar bar = bar(slender, StaminaConfig.DEFAULT, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);

        bar.consume();
        bar.consume();

        slenderSounds.assertEmpty();
        bar.stop();
    }

    /** Draws a hit of the lowest damage against a calm survivor: 0.6 hearts, 6 points. */
    private static final double[] HIT = {0.99D, 0.0D};
    private static final double[] MISS = {0.0D};
    private static final double HIT_POINTS = 0.6D * SlenderSwing.POINTS_PER_HEART;
    private static final double DELTA = 1.0E-9D;

    private static SlenderBar bar(CygnusPlayer slender, StaminaConfig config, LongSupplier clock, double[] rolls) {
        return (SlenderBar) StaminaFactory.createSlenderStamina(slender, config, clock, _ -> 0.0D,
                new SlenderSwing(() -> new FixedRandom(rolls)));
    }

    private CygnusPlayer slender(Env env, Pos where) {
        CygnusPlayer slender = (CygnusPlayer) env.createConnection().connect(instance, where);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        return slender;
    }

    private CygnusPlayer survivor(Env env, Pos where) {
        CygnusPlayer survivor = (CygnusPlayer) env.createConnection().connect(instance, where);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        return survivor;
    }

    private CygnusPlayer drainSwing(Env env, Instance instance, StaminaConfig config, double distance) {
        CygnusPlayer slender = slender(env, new Pos(0, 40, 0));
        CygnusPlayer survivor = survivor(env, new Pos(distance, 40, 0));
        SlenderBar bar = bar(slender, config, () -> 0L, HIT);
        bar.start();
        bar.changeStatus();
        bar.consume();
        bar.consume();
        bar.stop();
        return survivor;
    }
}
