package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.event.PageSpawnEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.page.PageProximityService;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamCreator;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the first page spawn is delayed rather than happening synchronously with
 * {@link GameStartEvent}, so survivors get a moment to move away from the spawn point first, and
 * that the delay is jittered within {@code GameConfig.PAGE_SPAWN_DELAY} ±
 * {@code GameConfig.PAGE_SPAWN_DELAY_JITTER} rather than landing on the exact same tick every round.
 */
class GameStartListenerTest extends CygnusPlayerTestBase {

    private static final int TICKS_PER_SECOND = 20;

    @Test
    void pageSpawnEventNeverFiresBeforeTheMinimumJitteredDelay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        GameStartListener listener = createListener(env, instance);

        AtomicBoolean pageSpawnFired = new AtomicBoolean(false);
        env.process().eventHandler().addListener(PageSpawnEvent.class, event -> pageSpawnFired.set(true));

        listener.accept(new GameStartEvent());
        // No random source is injectable here, but the jitter can never push the delay below
        // PAGE_SPAWN_DELAY - PAGE_SPAWN_DELAY_JITTER regardless of what gets rolled.
        int minDelayTicks = (GameConfig.PAGE_SPAWN_DELAY - GameConfig.PAGE_SPAWN_DELAY_JITTER) * TICKS_PER_SECOND;
        for (int i = 0; i < minDelayTicks - 1; i++) env.tick();
        assertFalse(pageSpawnFired.get(), "the page spawn must not fire before the minimum jittered delay");

        // Drain the scheduled task here (whatever it actually rolled), unasserted, so it can't leak
        // into a later test sharing this Env.
        int maxDelayTicks = (GameConfig.PAGE_SPAWN_DELAY + GameConfig.PAGE_SPAWN_DELAY_JITTER) * TICKS_PER_SECOND;
        for (int i = minDelayTicks - 1; i < maxDelayTicks + 5; i++) env.tick();

        env.destroyInstance(instance, true);
    }

    @Test
    void pageSpawnEventAlwaysFiresByTheMaximumJitteredDelay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        GameStartListener listener = createListener(env, instance);

        AtomicBoolean pageSpawnFired = new AtomicBoolean(false);
        env.process().eventHandler().addListener(PageSpawnEvent.class, event -> pageSpawnFired.set(true));

        listener.accept(new GameStartEvent());
        // Jitter can only push the delay later, never past PAGE_SPAWN_DELAY + PAGE_SPAWN_DELAY_JITTER.
        int maxDelayTicks = (GameConfig.PAGE_SPAWN_DELAY + GameConfig.PAGE_SPAWN_DELAY_JITTER) * TICKS_PER_SECOND;
        for (int i = 0; i < maxDelayTicks + 5; i++) env.tick();

        assertTrue(pageSpawnFired.get(), "the page spawn must fire once the configured delay has passed");

        env.destroyInstance(instance, true);
    }

    private static GameStartListener createListener(Env env, Instance instance) {
        CygnusPlayer slender = (CygnusPlayer) env.createPlayer(instance);
        CygnusPlayer survivor = (CygnusPlayer) env.createPlayer(instance);

        GameConfig gameConfig = new GameConfigReader(Paths.get("")).getConfig();
        TeamService teamService = TeamService.of();
        TeamCreator teamCreator = new TeamCreator() {};
        teamCreator.createTeams(gameConfig, teamService);

        Team slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow();
        Team survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow();
        slenderTeam.addPlayer(slender);
        survivorTeam.addPlayer(survivor);

        AmbientProvider ambientProvider = new AmbientProvider(survivorTeam);
        StaminaService staminaService = new StaminaService();
        PageProvider pageProvider = new PageProvider();
        PageProximityService pageProximityService = new PageProximityService(
                gameConfig,
                survivorTeam::getPlayers,
                List::of
        );

        return new GameStartListener(teamService, ambientProvider, staminaService, pageProvider, pageProximityService);
    }
}
