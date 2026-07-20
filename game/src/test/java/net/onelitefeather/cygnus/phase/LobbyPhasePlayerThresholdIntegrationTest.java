package net.onelitefeather.cygnus.phase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying the state transitions and player threshold handling
 * inside {@link LobbyPhase} and its waiting display.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.6
 **/
class LobbyPhasePlayerThresholdIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testLobbyPhaseStateTransitionsAndWaitingDisplay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        GameConfig config = GameConfig.builder()
                .lobbyTime(30)
                .minPlayers(2)
                .gameTime(600)
                .maxPlayers(10)
                .build();

        LobbyPhase lobbyPhase = new LobbyPhase(config);

        assertTrue(lobbyPhase.isPaused(), "Lobby phase should start paused.");
        assertEquals(30, lobbyPhase.getCurrentTicks(), "Lobby ticks should start at configured lobby time.");

        TestConnection connection1 = env.createConnection();
        Player player1 = connection1.connect(instance);
        Collector<ActionBarPacket> packets1 = connection1.trackIncoming(ActionBarPacket.class);

        lobbyPhase.setLevel(player1);
        lobbyPhase.checkStartCondition();

        assertTrue(lobbyPhase.isPaused(), "Lobby phase should remain paused with only 1 player.");

        List<ActionBarPacket> p1List = packets1.collect();
        assertFalse(p1List.isEmpty(), "Player 1 should have received an ActionBarPacket.");
        Component lastSentComponent = p1List.getLast().components().stream().findAny().orElse(null);
        assertNotNull(lastSentComponent, "ActionBarPacket should contain a component.");
        String plainText = PlainTextComponentSerializer.plainText().serialize(lastSentComponent);
        assertEquals("Need 1 players to start", plainText, "Action bar should request 1 player.");

        TestConnection connection2 = env.createConnection();
        Player player2 = connection2.connect(instance);

        lobbyPhase.setLevel(player2);
        lobbyPhase.checkStartCondition();

        assertFalse(lobbyPhase.isPaused(), "Lobby phase should unpause when minimum player threshold is met.");

        player2.remove(); // Removes the player from connection manager
        lobbyPhase.checkStopCondition();

        assertTrue(lobbyPhase.isPaused(), "Lobby phase should pause when player count drops below threshold.");
        assertEquals(30, lobbyPhase.getCurrentTicks(), "Lobby ticks should reset to lobby time.");

        env.destroyInstance(instance, true);
    }
}
