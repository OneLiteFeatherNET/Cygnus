package net.onelitefeather.cygnus.utils;

import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.TeamCreator;
import net.onelitefeather.cygnus.ambient.AmbientProvider;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.onelitefeather.cygnus.stamina.StaminaService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class StaminaHelperTest {

    @Disabled
    @Test
    void testStaminaObjectCreation(@NotNull Env env) {
        Instance testInstance = env.createFlatInstance();
        GameConfig gameConfig = new GameConfigReader(Paths.get("")).getConfig();
        AmbientProvider ambientProvider = new AmbientProvider();
        TeamService teamService = TeamService.of();
        TeamCreator teamCreator = new TeamCreator() {};
        teamCreator.createTeams(gameConfig, teamService, ambientProvider);
        StaminaService staminaService = new StaminaService();

        assertNotNull(staminaService);

        StaminaHelper.initStaminaObjects(teamService, staminaService);
        //TODO: Add assertions
    }

}