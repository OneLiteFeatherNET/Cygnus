package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.listener.game.SlenderTakeover;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuitListenerTest extends CygnusPlayerTestBase {

    private EventNode<Event> testNode;
    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        testNode = EventNode.all("test-node-" + UUID.randomUUID());
        env.process().eventHandler().addChild(testNode);
        instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(@NotNull Env env) {
        env.process().eventHandler().removeChild(testNode);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorDisconnectDoesNotEndTheMatch(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        Player slender = env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);
        teamService.add(spectatorTeam);

        slenderTeam.addPlayer(slender);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        spectatorTeam.addPlayer(spectator);
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), () -> {},
                new SlenderTakeover(teamService, () -> gamePhase, 1));
        testNode.addListener(PlayerDisconnectEvent.class, listener);

        AtomicBoolean finishFired = new AtomicBoolean(false);
        testNode.addListener(GameFinishEvent.class, event -> finishFired.set(true));

        playerQuit(spectator, env);

        assertFalse(finishFired.get(), "A disconnecting spectator must not trigger match end.");
        assertFalse(spectatorTeam.getPlayers().contains(spectator), "The spectator must still be removed from the spectator team.");
    }

    @Test
    void testLastSurvivorDisconnectStillEndsTheMatch(@NotNull Env env) {
        Player survivor = env.createPlayer(instance);
        Player slender = env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        slenderTeam.addPlayer(slender);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), () -> {},
                new SlenderTakeover(teamService, () -> gamePhase, 1));
        testNode.addListener(PlayerDisconnectEvent.class, listener);

        AtomicReference<GameFinishEvent> finishEvent = new AtomicReference<>();
        testNode.addListener(GameFinishEvent.class, finishEvent::set);

        playerQuit(survivor, env);

        assertNotNull(finishEvent.get(), "GameFinishEvent must be dispatched");
        assertEquals(GameFinishEvent.Reason.SURVIVOR_LEFT, finishEvent.get().reason());
    }

    @Test
    void testSlenderDisconnectTriggersReviveWhenEnoughSurvivors(@NotNull Env env) {
        Player survivor1 = env.createPlayer(instance);
        Player survivor2 = env.createPlayer(instance);
        Player slender = env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        slenderTeam.addPlayer(slender);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivorTeam.addPlayer(survivor1);
        survivor1.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        survivorTeam.addPlayer(survivor2);
        survivor2.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), () -> {},
                new SlenderTakeover(teamService, () -> gamePhase, 1));
        testNode.addListener(PlayerDisconnectEvent.class, listener);

        AtomicReference<SlenderReviveEvent> reviveEvent = new AtomicReference<>();
        testNode.addListener(SlenderReviveEvent.class, reviveEvent::set);

        playerQuit(slender, env);

        // The takeover itself, after its countdown, is covered by SlenderTakeoverTest
        assertNull(reviveEvent.get(), "the chosen survivor is warned first, the takeover follows the countdown");
        assertTrue(survivor1.hasEffect(PotionEffect.BLINDNESS) || survivor2.hasEffect(PotionEffect.BLINDNESS),
                "one survivor must have been chosen and blinded for the countdown");
        assertEquals(2, survivorTeam.getCurrentSize(), "the chosen survivor stays a survivor during the countdown");
    }

    @Test
    void testSlenderDisconnectEndsMatchWhenNotEnoughSurvivors(@NotNull Env env) {
        Player survivor = env.createPlayer(instance);
        Player slender = env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        slenderTeam.addPlayer(slender);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), () -> {},
                new SlenderTakeover(teamService, () -> gamePhase, 1));
        testNode.addListener(PlayerDisconnectEvent.class, listener);

        AtomicReference<GameFinishEvent> finishEvent = new AtomicReference<>();
        testNode.addListener(GameFinishEvent.class, finishEvent::set);

        playerQuit(slender, env);

        assertNotNull(finishEvent.get(), "GameFinishEvent must be dispatched");
        assertEquals(GameFinishEvent.Reason.SLENDER_LEFT, finishEvent.get().reason());
    }

    private static void playerQuit(Player player, Env env) {
        env.process().eventHandler().call(new PlayerDisconnectEvent(player));
    }
}
