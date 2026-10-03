package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetTitleTextPacket;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlenderTakeoverTest extends CygnusPlayerTestBase {

    private TeamService teamService;
    private Team slenderTeam;
    private Team survivorTeam;
    private GamePhase gamePhase;
    private final AtomicReference<SlenderReviveEvent> revive = new AtomicReference<>();
    private final AtomicReference<GameFinishEvent> finish = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        this.teamService = TeamService.of();
        this.slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        this.survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        this.teamService.add(this.slenderTeam);
        this.teamService.add(this.survivorTeam);
        this.gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager(), _ -> 0.0D);
    }

    private SlenderTakeover takeover(Env env) {
        SlenderTakeover takeover = new SlenderTakeover(this.teamService, () -> this.gamePhase);
        env.process().eventHandler().addChild(takeover.node());
        env.process().eventHandler().addListener(SlenderReviveEvent.class, this.revive::set);
        env.process().eventHandler().addListener(GameFinishEvent.class, this.finish::set);
        return takeover;
    }

    private Player survivor(Env env, Instance instance) {
        Player player = env.createPlayer(instance);
        this.survivorTeam.addPlayer(player);
        return player;
    }

    private void countdown(SlenderTakeover takeover) {
        for (int i = 0; i < SlenderTakeover.COUNTDOWN_SECONDS; i++) {
            takeover.tick();
        }
    }

    @Test
    @DisplayName("The chosen survivor stays a survivor until the countdown is over")
    void switchesOnlyAfterTheCountdown(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);

        takeover.begin(this.gamePhase);
        Player chosen = takeover.chosen();
        assertNotNull(chosen);
        assertTrue(chosen.hasEffect(PotionEffect.BLINDNESS));

        for (int i = 0; i < SlenderTakeover.COUNTDOWN_SECONDS - 1; i++) {
            takeover.tick();
        }
        assertTrue(this.survivorTeam.getPlayers().contains(chosen));
        assertNull(this.revive.get());

        takeover.tick();

        assertTrue(this.slenderTeam.getPlayers().contains(chosen));
        assertFalse(this.survivorTeam.getPlayers().contains(chosen));
        assertEquals(chosen, this.revive.get().getPlayer());
        assertNull(takeover.chosen());
    }

    @Test
    @DisplayName("Only the chosen survivor sees the announcement")
    void announcesToTheChosenOnly(Env env) {
        Instance instance = env.createFlatInstance();
        Map<Player, Collector<SetTitleTextPacket>> titles = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            this.survivorTeam.addPlayer(player);
            titles.put(player, connection.trackIncoming(SetTitleTextPacket.class));
        }
        SlenderTakeover takeover = takeover(env);

        takeover.begin(this.gamePhase);
        Player chosen = takeover.chosen();

        titles.forEach((player, collector) -> {
            if (player == chosen) {
                collector.assertSingle(packet -> assertEquals(Messages.SLENDER_TAKEOVER_TITLE, packet.title()));
            } else {
                collector.assertEmpty();
            }
        });
    }

    @Test
    @DisplayName("When the chosen survivor dies, another one is chosen")
    void deathChoosesAgain(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);
        takeover.begin(this.gamePhase);
        Player first = takeover.chosen();

        // What PlayerDeathListener does before the takeover hears about it
        this.survivorTeam.removePlayer(first);
        EventDispatcher.call(new PlayerDeathEvent(first, Component.empty(), Component.empty()));

        Player second = takeover.chosen();
        assertNotNull(second);
        assertNotEquals(first, second);
        countdown(takeover);
        assertEquals(second, this.revive.get().getPlayer());
    }

    @Test
    @DisplayName("Without anyone left to take over, the round ends")
    void noSuccessorEndsTheRound(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);
        takeover.begin(this.gamePhase);
        Player chosen = takeover.chosen();

        this.survivorTeam.removePlayer(chosen);
        EventDispatcher.call(new PlayerDeathEvent(chosen, Component.empty(), Component.empty()));

        assertNotNull(this.finish.get());
        assertEquals(GameFinishEvent.Reason.SLENDER_LEFT, this.finish.get().reason());
        assertNull(takeover.chosen());
    }

    @Test
    @DisplayName("The end of the round calls the takeover off")
    void finishCancels(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);
        takeover.begin(this.gamePhase);

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));
        countdown(takeover);

        assertNull(this.revive.get());
        assertNull(takeover.chosen());
    }

    @Test
    @DisplayName("Nobody becomes the slender once nobody would be left to hunt")
    void lastSurvivorStanding(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);
        takeover.begin(this.gamePhase);
        Player chosen = takeover.chosen();
        Player other = this.survivorTeam.getPlayers().stream().filter(player -> player != chosen).findFirst().orElseThrow();

        // The other survivor dies during the countdown, the creek does not wait
        this.survivorTeam.removePlayer(other);
        EventDispatcher.call(new PlayerDeathEvent(other, Component.empty(), Component.empty()));
        countdown(takeover);

        assertNull(this.revive.get());
        assertNotNull(this.finish.get());
        assertEquals(GameFinishEvent.Reason.SLENDER_LEFT, this.finish.get().reason());
    }

    @Test
    @DisplayName("A round gets one takeover only, however many survivors are left")
    void oneTakeoverPerRound(Env env) {
        Instance instance = env.createFlatInstance();
        survivor(env, instance);
        survivor(env, instance);
        survivor(env, instance);
        SlenderTakeover takeover = takeover(env);
        takeover.begin(this.gamePhase);
        countdown(takeover);
        assertNotNull(this.revive.get());

        // The new slender leaves as well, two survivors would still be there to take over
        takeover.begin(this.gamePhase);

        assertNull(takeover.chosen());
        assertNotNull(this.finish.get());
        assertEquals(GameFinishEvent.Reason.SLENDER_LEFT, this.finish.get().reason());
    }
}
