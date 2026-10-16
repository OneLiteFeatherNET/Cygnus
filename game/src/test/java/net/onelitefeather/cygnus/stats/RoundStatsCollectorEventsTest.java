package net.onelitefeather.cygnus.stats;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.PainkillerUsedEvent;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.stats.RoundStats.Outcome;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the event adapter of the collector with real Minestom events on the test environment's
 * handler. The ticks are not needed: the events are dispatched synchronously.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class RoundStatsCollectorEventsTest extends CygnusPlayerTestBase {

    private final List<FinishedRound> rounds = new ArrayList<>();
    private final StatsRepository repository = new StatsRepository() {
        @Override
        public void saveRound(FinishedRound round) {
            RoundStatsCollectorEventsTest.this.rounds.add(round);
        }

        @Override
        public int deleteByPlayer(UUID playerUuid) {
            return 0;
        }

        @Override
        public void close() {
        }
    };

    private Player player(Env env, Instance instance) {
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    private RoundStatsCollector registered(Env env, Player slender, Player alice, Player bob) {
        RoundStatsCollector collector = new RoundStatsCollector(this.repository,
                Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC), ZoneOffset.UTC, () -> UUID.randomUUID().toString());
        EventNode<Event> node = env.process().eventHandler();
        collector.register(node,
                () -> Map.of(slender.getUuid(), Role.SLENDER, alice.getUuid(), Role.SURVIVOR, bob.getUuid(), Role.SURVIVOR),
                slender::getUuid);
        return collector;
    }

    private RoundStats statsOf(Player player) {
        return this.rounds.getFirst().players().stream()
                .filter(stats -> stats.playerUuid().equals(player.getUuid()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no statistics for " + player.getUsername()));
    }

    @Test
    @DisplayName("The game's events end up in the participants' round statistics")
    void gameEventsAreCollected(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = player(env, instance);
        Player alice = player(env, instance);
        Player bob = player(env, instance);
        registered(env, slender, alice, bob);

        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PageFoundEvent(alice, 8, 8));
        EventDispatcher.call(new PlayerDamagedEvent(alice, alice.getPosition(), 12.5D));
        EventDispatcher.call(new PainkillerUsedEvent(alice));
        EventDispatcher.call(new PlayerDeathEvent(bob, null, null));
        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));

        assertEquals(1, this.rounds.size());
        RoundStats aliceStats = statsOf(alice);
        assertEquals(1, aliceStats.pagesFound());
        assertTrue(aliceStats.finisher(), "page 8 of 8");
        assertEquals(12.5D, aliceStats.damageTaken(), 1.0E-9);
        assertEquals(1, aliceStats.painkillersUsed());
        assertTrue(statsOf(bob).died());
        assertEquals(1, statsOf(slender).kills(), "the death is credited to the Slender");
        assertEquals(Outcome.WIN, aliceStats.outcome());
    }

    @Test
    @DisplayName("A disconnect before the finish makes the player abandoned")
    void disconnectIsAbandoned(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = player(env, instance);
        Player alice = player(env, instance);
        Player bob = player(env, instance);
        registered(env, slender, alice, bob);

        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new PlayerDisconnectEvent(bob));
        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertEquals(Outcome.ABANDONED, statsOf(bob).outcome());
        assertEquals(Outcome.WIN, statsOf(alice).outcome());
    }

    @Test
    @DisplayName("A Slender revive marks the survivor as taken over and keeps the start role")
    void reviveMarksTheTakeover(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = player(env, instance);
        Player alice = player(env, instance);
        Player bob = player(env, instance);
        registered(env, slender, alice, bob);

        EventDispatcher.call(new GameStartEvent());
        EventDispatcher.call(new SlenderReviveEvent(alice));
        env.tick();
        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertTrue(statsOf(alice).takenOver(), "Alice became the Slender");
        assertEquals(Role.SURVIVOR, statsOf(alice).role(), "the role stays the start role");
        assertFalse(statsOf(bob).takenOver(), "Bob did not take over");
        assertEquals(3, statsOf(bob).participants(), "the lobby had three participants");
    }
}
