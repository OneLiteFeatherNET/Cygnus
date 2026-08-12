package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.hud.PageCountHudComponent;
import net.onelitefeather.cygnus.hud.PageTimerHudComponent;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerChatListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSpectatorMessageDuringGamePhaseOnlyReachesSpectators(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        spectator.setDisplayName(Component.text(spectator.getUsername()));
        survivor.setDisplayName(Component.text(survivor.getUsername()));

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        spectatorTeam.addPlayer(spectator);
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        GamePhase gamePhase = new GamePhase(new PageTimerHudComponent(), new PageCountHudComponent(), () -> {}, 600, new JumpScareManager());
        PlayerChatListener listener = new PlayerChatListener(spectatorTeam, () -> gamePhase);
        PlayerChatEvent event = new PlayerChatEvent(spectator, List.of(spectator, survivor), "hi");

        listener.accept(event);

        assertTrue(event.getRecipients().isEmpty(), "Default broadcast recipients must be cleared for a spectator sender.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSurvivorMessageDuringGamePhaseReachesEveryone(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        survivor.setDisplayName(Component.text(survivor.getUsername()));
        spectator.setDisplayName(Component.text(spectator.getUsername()));

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        GamePhase gamePhase = new GamePhase(new PageTimerHudComponent(), new PageCountHudComponent(), () -> {}, 600, new JumpScareManager());
        PlayerChatListener listener = new PlayerChatListener(spectatorTeam, () -> gamePhase);

        PlayerChatEvent event = new PlayerChatEvent(survivor, List.of(survivor, spectator), "hi");

        listener.accept(event);

        assertEquals(2, event.getRecipients().size(), "A non-spectator sender's message must keep the default recipient list.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorMessageOutsideGamePhaseIsUnrestricted(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        spectator.setDisplayName(Component.text(spectator.getUsername()));
        other.setDisplayName(Component.text(other.getUsername()));

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        spectatorTeam.addPlayer(spectator);
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        GameConfig config = GameConfig.builder().lobbyTime(30).minPlayers(2).gameTime(600).maxPlayers(10).build();
        LobbyPhase lobbyPhase = new LobbyPhase(config);
        PlayerChatListener listener = new PlayerChatListener(spectatorTeam, () -> lobbyPhase);

        PlayerChatEvent event = new PlayerChatEvent(spectator, List.of(spectator, other), "hi");

        listener.accept(event);

        assertEquals(2, event.getRecipients().size(), "Outside GamePhase, chat must remain unrestricted regardless of stale team tags.");

        env.destroyInstance(instance, true);
    }
}
