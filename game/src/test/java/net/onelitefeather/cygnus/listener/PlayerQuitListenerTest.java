package net.onelitefeather.cygnus.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerQuitListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSpectatorDisconnectDoesNotEndTheMatch(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
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
        slender.setTag(Tags.TEAM_ID, TeamHelper.SLENDER_TEAM_ID);
        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_ID, TeamHelper.SURVIVOR_TEAM_ID);
        spectatorTeam.addPlayer(spectator);
        spectator.setTag(Tags.TEAM_ID, TeamHelper.SPECTATOR_TEAM_ID);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), 2);

        // Cyano's FlexibleListener#failFollowup() can't cleanly express "this event never
        // fires" here (clearing the guard afterward re-triggers the same check), so this
        // negative assertion stays on a plain listener instead of env.listen(...).
        AtomicBoolean finishFired = new AtomicBoolean(false);
        MinecraftServer.getGlobalEventHandler().addListener(GameFinishEvent.class, event -> finishFired.set(true));

        listener.accept(new PlayerDisconnectEvent(spectator));

        assertFalse(finishFired.get(), "A disconnecting spectator must not trigger match end.");
        assertFalse(spectatorTeam.getPlayers().contains(spectator), "The spectator must still be removed from the spectator team.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testLastSurvivorDisconnectStillEndsTheMatch(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
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
        slender.setTag(Tags.TEAM_ID, TeamHelper.SLENDER_TEAM_ID);
        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_ID, TeamHelper.SURVIVOR_TEAM_ID);

        GamePhase gamePhase = new GamePhase(new GameViewImpl(), () -> {}, 600, new JumpScareManager());
        PlayerQuitListener listener = new PlayerQuitListener(() -> gamePhase, teamService, new StaminaService(), 2);

        env.listen(GameFinishEvent.class)
                .followup(event -> assertEquals(GameFinishEvent.Reason.SURVIVOR_LEFT, event.reason()));

        listener.accept(new PlayerDisconnectEvent(survivor));

        env.destroyInstance(instance, true);
    }
}
