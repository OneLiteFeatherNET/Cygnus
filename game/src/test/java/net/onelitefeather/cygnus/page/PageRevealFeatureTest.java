package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Operation;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Target;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.PageProximityTarget;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.stamina.StaminaFactory;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRevealFeatureTest extends CygnusPlayerTestBase {

    /**
     * Always rolls 0: every waypoint lies half the spread off its page, straight along x, so the
     * tests can tell where it points.
     */
    private static final RandomGenerator NO_TURN = () -> 0L;

    /** How far {@link #NO_TURN} moves a waypoint along x. */
    private static final int SHIFT = (int) (PageRevealFeature.SPREAD_BLOCKS / 2.0D);

    private static final int SHOWN_TICKS = PageRevealFeature.SECONDS * 20;

    private final AtomicLong clock = new AtomicLong();
    private final List<PageProximityTarget> pages = new ArrayList<>();
    private final Set<Player> survivors = new HashSet<>();
    private final Map<Player, FoodBar> staminaBars = new HashMap<>();
    private @Nullable PageRevealFeature feature;

    private PageRevealFeature feature(Env env) {
        return feature(env, NO_TURN);
    }

    private PageRevealFeature feature(Env env, RandomGenerator random) {
        PageRevealFeature feature = new PageRevealFeature(() -> this.pages, () -> this.survivors,
                this.staminaBars::get, this.clock::get, TickSections.NONE, random);
        env.process().eventHandler().addChild(feature.node());
        this.feature = feature;
        return feature;
    }

    // The server process is shared by the whole class, so a feature left behind would answer the next test's events
    @AfterEach
    void detach(Env env) {
        this.staminaBars.values().forEach(FoodBar::stop);
        this.staminaBars.clear();
        if (this.feature == null) return;
        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        env.process().eventHandler().removeChild(this.feature.node());
    }

    private Player survivor(Env env, TestConnection connection, Instance instance) {
        Player survivor = connection.connect(instance, new Pos(0, 40, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        this.survivors.add(survivor);
        return survivor;
    }

    private PageProximityTarget page(Pos position) {
        PageProximityTarget page = PageProximityTarget.of(UUID.randomUUID(), position, 1.0D);
        this.pages.add(page);
        return page;
    }

    private static ItemStack revealItem() {
        return ItemStack.builder(Items.PAGE_REVEAL_MATERIAL).set(Tags.ITEM_TAG, Items.PAGE_REVEAL_ITEM).build();
    }

    private static void use(Player player) {
        EventDispatcher.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, revealItem(), 0L));
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    private static List<Operation> operations(Collector<TrackedWaypointPacket> packets) {
        return packets.collect().stream().map(TrackedWaypointPacket::operation).toList();
    }

    @Test
    @DisplayName("A round start puts the item in the survivors' second to last slot")
    void roundStartGivesTheItem(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = survivor(env, env.createConnection(), instance);
        feature(env);

        EventDispatcher.call(new GameStartEvent());

        ItemStack item = survivor.getInventory().getItemStack(Items.PAGE_REVEAL_SLOT);
        assertEquals(Items.PAGE_REVEAL_ITEM, item.getTag(Tags.ITEM_TAG));
        assertEquals(7, Items.PAGE_REVEAL_SLOT);
    }

    @Test
    @DisplayName("A use shows a page in range on the user's locator bar, and only on his")
    void useShowsNearbyPage(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection userConnection = env.createConnection();
        Player user = survivor(env, userConnection, instance);
        TestConnection otherConnection = env.createConnection();
        survivor(env, otherConnection, instance);
        PageProximityTarget page = page(new Pos(20, 41, 10));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        Collector<TrackedWaypointPacket> userPackets = userConnection.trackIncoming(TrackedWaypointPacket.class);
        Collector<TrackedWaypointPacket> otherPackets = otherConnection.trackIncoming(TrackedWaypointPacket.class);

        use(user);

        List<TrackedWaypointPacket> sent = userPackets.collect();
        assertEquals(1, sent.size());
        assertEquals(Operation.TRACK, sent.getFirst().operation());
        assertEquals(new Target.Vec3i(new Pos(20 + SHIFT, 41, 10)), sent.getFirst().waypoint().target());
        assertTrue(otherPackets.collect().isEmpty());
        assertFalse(sent.getFirst().waypoint().id().unify(page.id()::equals, _ -> false),
                "the page's own id would let the client draw the waypoint on the page entity itself");
    }

    @Test
    @DisplayName("A page outside the radius stays off the locator bar")
    void farPageIsLeftOut(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(PageRevealFeature.RADIUS + 1, 40, 0));
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        use(user);

        List<TrackedWaypointPacket> sent = packets.collect();
        assertEquals(1, sent.size());
        assertEquals(new Target.Vec3i(new Pos(10 + SHIFT, 40, 0)), sent.getFirst().waypoint().target());
    }

    @Test
    @DisplayName("The waypoint lies within the spread around the page, but not on it")
    void waypointIsApproximate(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(20, 40, 20));
        feature(env, new SplittableRandom(7L));
        EventDispatcher.call(new GameStartEvent());
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        use(user);

        Target.Vec3i target = (Target.Vec3i) packets.collect().getFirst().waypoint().target();
        Pos spot = new Pos(target.point().x(), target.point().y(), target.point().z());
        assertEquals(40, spot.blockY());
        // The block coordinates round down, so up to one block more or less in each direction.
        double distance = spot.distance(new Pos(20, 40, 20));
        assertTrue(distance <= PageRevealFeature.SPREAD_BLOCKS + Math.sqrt(2.0D), "too far off: " + spot);
        assertTrue(distance >= PageRevealFeature.SPREAD_BLOCKS / 2.0D - Math.sqrt(2.0D), "too close: " + spot);
    }

    @Test
    @DisplayName("A use starts the cooldown on the item, and the next use waits for it")
    void cooldownBlocksTheNextUse(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        Collector<SetCooldownPacket> cooldowns = connection.trackIncoming(SetCooldownPacket.class);

        use(user);

        List<SetCooldownPacket> sent = cooldowns.collect();
        assertEquals(1, sent.size());
        assertEquals(Items.PAGE_REVEAL_MATERIAL.key().asString(), sent.getFirst().cooldownGroup());
        assertEquals(PageRevealFeature.COOLDOWN_SECONDS * 20, sent.getFirst().cooldownTicks());

        tick(env, SHOWN_TICKS + PageRevealFeature.RESEND_TICKS);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);
        this.clock.addAndGet((PageRevealFeature.COOLDOWN_SECONDS - 1) * 1000L);
        use(user);
        assertTrue(packets.collect().isEmpty());

        this.clock.addAndGet(1_000L);
        Collector<TrackedWaypointPacket> later = connection.trackIncoming(TrackedWaypointPacket.class);
        use(user);
        assertEquals(List.of(Operation.TRACK), operations(later));
    }

    @Test
    @DisplayName("Without a page in range the survivor is told so and no cooldown starts")
    void nothingFoundCostsNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        Collector<ActionBarPacket> messages = connection.trackIncoming(ActionBarPacket.class);
        Collector<SetCooldownPacket> cooldowns = connection.trackIncoming(SetCooldownPacket.class);

        use(user);

        assertEquals(List.of(new ActionBarPacket(PageRevealFeature.NOTHING_FOUND)), messages.collect());
        assertTrue(cooldowns.collect().isEmpty());

        page(new Pos(10, 40, 0));
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);
        use(user);
        assertEquals(List.of(Operation.TRACK), operations(packets));
    }

    @Test
    @DisplayName("A use while sprinting only tells the survivor to stop, no cooldown starts")
    void sprintingUseCostsNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        user.setSprinting(true);
        Collector<ActionBarPacket> messages = connection.trackIncoming(ActionBarPacket.class);
        Collector<SetCooldownPacket> cooldowns = connection.trackIncoming(SetCooldownPacket.class);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        use(user);

        assertEquals(List.of(new ActionBarPacket(PageRevealFeature.SPRINTING)), messages.collect());
        assertTrue(cooldowns.collect().isEmpty());
        assertTrue(packets.collect().isEmpty());

        user.setSprinting(false);
        Collector<TrackedWaypointPacket> standing = connection.trackIncoming(TrackedWaypointPacket.class);
        use(user);
        assertEquals(List.of(Operation.TRACK), operations(standing));
    }

    @Test
    @DisplayName("A sprint takes the pages off the locator bar for good")
    void sprintUntracks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        page(new Pos(-10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        user.setSprinting(true);
        tick(env, PageRevealFeature.RESEND_TICKS);

        assertEquals(List.of(Operation.UNTRACK, Operation.UNTRACK), operations(packets));

        user.setSprinting(false);
        Collector<TrackedWaypointPacket> later = connection.trackIncoming(TrackedWaypointPacket.class);
        tick(env, SHOWN_TICKS);
        assertTrue(later.collect().isEmpty(), "stopping again does not bring the pages back");
    }

    @Test
    @DisplayName("The stamina stays off the experience bar while the pages are shown")
    void staminaDisplayIsHeld(Env env) {
        Instance instance = env.createFlatInstance();
        Player user = survivor(env, env.createConnection(), instance);
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina((CygnusPlayer) user);
        bar.start();
        this.staminaBars.put(user, bar);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());

        use(user);
        bar.startConsume();
        bar.consume();

        assertEquals(1.0f, user.getExp(), 1.0E-6f, "a change of the stamina must not push the locator bar away");

        user.setSprinting(true);
        tick(env, PageRevealFeature.RESEND_TICKS);

        assertEquals(bar.remainingShare(), user.getExp(), 1.0E-6f);
    }

    @Test
    @DisplayName("The pages leave the locator bar once the time is up")
    void timeUpUntracks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        tick(env, SHOWN_TICKS - PageRevealFeature.RESEND_TICKS);
        assertFalse(operations(packets).contains(Operation.UNTRACK));

        Collector<TrackedWaypointPacket> later = connection.trackIncoming(TrackedWaypointPacket.class);
        tick(env, 2 * PageRevealFeature.RESEND_TICKS);
        assertEquals(List.of(Operation.UNTRACK), operations(later));
    }

    @Test
    @DisplayName("A page found meanwhile leaves the locator bar at once")
    void foundPageUntracks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        PageProximityTarget page = page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        this.pages.remove(page);
        tick(env, PageRevealFeature.RESEND_TICKS);

        assertEquals(List.of(Operation.UNTRACK), operations(packets));
    }

    @Test
    @DisplayName("A page that moved meanwhile takes its waypoint along")
    void movedPageUpdates(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        PageProximityTarget page = page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        this.pages.remove(page);
        this.pages.add(PageProximityTarget.of(page.id(), new Pos(15, 42, 3), 1.0D));
        tick(env, PageRevealFeature.RESEND_TICKS);

        List<TrackedWaypointPacket> sent = packets.collect();
        assertEquals(1, sent.size());
        assertEquals(Operation.UPDATE, sent.getFirst().operation());
        assertEquals(new Target.Vec3i(new Pos(15 + SHIFT, 42, 3)), sent.getFirst().waypoint().target());
    }

    @Test
    @DisplayName("A survivor who dies loses his waypoints")
    void deathUntracks(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        EventDispatcher.call(new PlayerDeathEvent(user, Component.empty(), Component.empty()));

        assertEquals(List.of(Operation.UNTRACK), operations(packets));
    }

    @Test
    @DisplayName("The round end takes every waypoint away and forgets the cooldowns")
    void roundEndClears(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player user = survivor(env, connection, instance);
        page(new Pos(10, 40, 0));
        feature(env);
        EventDispatcher.call(new GameStartEvent());
        use(user);
        Collector<TrackedWaypointPacket> packets = connection.trackIncoming(TrackedWaypointPacket.class);

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        assertEquals(List.of(Operation.UNTRACK), operations(packets));

        EventDispatcher.call(new GameStartEvent());
        Collector<TrackedWaypointPacket> nextRound = connection.trackIncoming(TrackedWaypointPacket.class);
        use(user);
        assertEquals(List.of(Operation.TRACK), operations(nextRound));
    }

    @Test
    @DisplayName("The item does nothing for the slender or outside a round")
    void onlySurvivorsDuringRound(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivor(env, survivorConnection, instance);
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        page(new Pos(10, 40, 0));
        feature(env);
        Collector<TrackedWaypointPacket> survivorPackets = survivorConnection.trackIncoming(TrackedWaypointPacket.class);
        Collector<TrackedWaypointPacket> slenderPackets = slenderConnection.trackIncoming(TrackedWaypointPacket.class);

        use(survivor);
        EventDispatcher.call(new GameStartEvent());
        use(slender);

        assertTrue(survivorPackets.collect().isEmpty());
        assertTrue(slenderPackets.collect().isEmpty());
    }
}
