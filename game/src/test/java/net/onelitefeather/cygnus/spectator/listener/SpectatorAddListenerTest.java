package net.onelitefeather.cygnus.spectator.listener;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import net.onelitefeather.cygnus.player.listener.SpectatorAddListener;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorAddListenerTest extends CygnusPlayerTestBase {

    @Test
    void testAcceptConvertsThePlayerIntoASpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService spectatorService = new SpectatorService(spectatorTeam, survivorTeam);
        SpectatorAddListener listener = new SpectatorAddListener(spectatorService);

        listener.accept(new SpectatorAddEvent(player));

        assertEquals(GameMode.SPECTATOR, player.getGameMode());
        assertTrue(spectatorTeam.getPlayers().contains(player));

        env.destroyInstance(instance, true);
    }
}
