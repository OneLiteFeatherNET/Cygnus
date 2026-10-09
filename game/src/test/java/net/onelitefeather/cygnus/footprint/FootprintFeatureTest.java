package net.onelitefeather.cygnus.footprint;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.utils.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootprintFeatureTest extends CygnusPlayerTestBase {

    private static final FootprintConfig CONFIG = new FootprintConfig(3.0D, 1.0D, 50, 50, 12,
            2.0D, 30, 40, 5, 40, 15, 120, 8.0D, 0.7D, 0.33D);

    private final AtomicLong clock = new AtomicLong();

    private FootprintFeature feature(Env env) {
        FootprintFeature feature = new FootprintFeature(CONFIG, this.clock::get, new FixedRandom(0.0D));
        env.process().eventHandler().addChild(feature.node());
        return feature;
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    /**
     * Walks one block per move along x. Dispatching the event does not move the player, so each
     * move is followed by a teleport, which fires no move event of its own.
     */
    private static void walk(Player player, int blocks) {
        for (int x = 0; x < blocks; x++) {
            Pos next = player.getPosition().add(1, 0, 0);
            EventDispatcher.call(new PlayerMoveEvent(player, next, true));
            player.teleport(next).join();
        }
    }

    private static ItemStack tracker() {
        return ItemStack.builder(Items.TRACKING_MATERIAL).set(Tags.ITEM_TAG, Items.TRACKING_ITEM).build();
    }

    @Test
    @DisplayName("Moves only count while a round runs")
    void onlyDuringRound(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);

        walk(survivor, 4);
        assertEquals(0, feature.log().size(survivor.getUuid()));

        EventDispatcher.call(new GameStartEvent());
        walk(survivor, 4);
        assertTrue(feature.log().size(survivor.getUuid()) > 0);
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Using the tracker reveals survivor prints")
    void trackerReveals(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 5.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(survivor, 6);
        this.clock.set(10_000L);

        EventDispatcher.call(new PlayerUseItemEvent(slender, PlayerHand.MAIN, tracker(), 0L));
        tick(env, 2);

        assertTrue(feature.spawner().live().size() > 0);
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A dead survivor's track is forgotten")
    void deathClearsTrack(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(survivor, 4);

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));

        assertEquals(0, feature.log().size(survivor.getUuid()));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A new slender sees the cooldown left from the old one")
    void takeoverKeepsCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        first.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PlayerUseItemEvent(first, PlayerHand.MAIN, tracker(), 0L));
        this.clock.set(30_000L);
        TestConnection connection = env.createConnection();
        Player next = connection.connect(instance, new Pos(2.5, 40, 0.5));
        Collector<ServerPacket> packets = connection.trackIncoming();

        EventDispatcher.call(new SlenderReviveEvent(next));

        assertTrue(packets.collect().stream().anyMatch(packet -> packet instanceof SetCooldownPacket cooldown
                && cooldown.cooldownTicks() == 90 * 20));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The round end leaves no prints and no tracks behind")
    void roundEndCleansUp(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 5.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(slender, 6);
        walk(survivor, 4);
        tick(env, 4);
        assertTrue(feature.spawner().live().size() > 0, "the hidden slender left prints first");

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertTrue(feature.spawner().live().isEmpty());
        assertEquals(0, feature.log().size(survivor.getUuid()));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A survivor who dies stops seeing the slender prints already lying around")
    void deathHidesPrints(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 5.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(slender, 3);
        tick(env, 4);
        Footprint footprint = feature.spawner().live().iterator().next();
        assertTrue(footprint.isShownTo(survivor));

        // The death listener takes the team away before the features hear of the death.
        survivor.removeTag(Tags.TEAM_KEY);
        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));

        assertFalse(footprint.isShownTo(survivor));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A survivor who takes over as slender does not find his own old track")
    void takeoverForgetsOwnTrack(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(survivor, 4);

        EventDispatcher.call(new SlenderReviveEvent(survivor));

        assertEquals(0, feature.log().size(survivor.getUuid()));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A survivor whose client reloads the chunk sees the slender print again")
    void chunkLoadResends(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, new Pos(0.5, 40, 0.5));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, new Pos(0.5, 40, 5.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        FootprintFeature feature = feature(env);
        EventDispatcher.call(new GameStartEvent());
        walk(slender, 3);
        tick(env, 4);
        Footprint footprint = feature.spawner().live().getFirst();
        Collector<BlockChangePacket> packets = connection.trackIncoming(BlockChangePacket.class);

        EventDispatcher.call(new PlayerChunkLoadEvent(survivor, footprint.block().chunkX(), footprint.block().chunkZ()));

        packets.assertSingle(packet -> assertEquals(footprint.state().stateId(), packet.blockStateId()));
        env.process().eventHandler().removeChild(feature.node());
        env.destroyInstance(instance, true);
    }
}
